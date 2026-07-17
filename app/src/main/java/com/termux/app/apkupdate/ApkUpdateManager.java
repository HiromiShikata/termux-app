package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.termux.BuildConfig;

import java.io.File;

public class ApkUpdateManager {

    public static final String PREFERENCE_KEY_AUTO_CHECK = "auto_check_for_updates";

    public static final String PREFERENCE_KEY_LAST_CHECK_TIME = "last_update_check_time";

    public static final long NO_CHECK_TIME = 0L;

    public interface CheckListener {
        void onUpdateAvailable(ApkUpdateAvailability availability);

        void onUpToDate(String latestVersionName);

        /**
         * Called when the check could not complete (network error, HTTP error, or an exhausted
         * GitHub rate limit even after the Atom-feed fallback). {@code rateLimited} is {@code true}
         * when the failure was a GitHub rate limit, so the caller can show a distinct
         * "rate limited, try later" message instead of conflating it with "no newer version".
         */
        void onCheckFailed(String message, boolean rateLimited);
    }

    public interface DownloadListener {
        void onDownloaded(File apkFile);

        void onDownloadFailed(String message);
    }

    public interface PreviousBuildsListener {
        void onPreviousBuilds(java.util.List<ApkRelease> previousBuilds);

        void onPreviousBuildsFailed(String message, boolean rateLimited);
    }

    private final Context context;
    private final ApkUpdateGuide updateGuide;
    private final GithubReleaseClient releaseClient;
    private final GithubAtomReleaseFeedParser atomReleaseFeedParser;
    private final AtomReleaseJsonSynthesizer atomReleaseJsonSynthesizer;
    private final ApkUpdatePlanner updatePlanner;
    private final GithubReleaseListParser releaseListParser;
    private final PreviousReleaseSelector previousReleaseSelector;
    private final ApkDownloader apkDownloader;
    private final Sha256SumsUrlResolver sha256SumsUrlResolver;
    private final Sha256SumsParser sha256SumsParser;
    private final ApkFileValidator apkFileValidator;
    private final Handler mainHandler;

    public ApkUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.updateGuide = new ApkUpdateGuide();
        this.releaseClient = new GithubReleaseClient();
        this.atomReleaseFeedParser = new GithubAtomReleaseFeedParser();
        this.atomReleaseJsonSynthesizer =
            new AtomReleaseJsonSynthesizer(updateGuide.getReleasesOwner(), updateGuide.getReleasesRepo());
        this.updatePlanner = new ApkUpdatePlanner();
        this.releaseListParser = new GithubReleaseListParser();
        this.previousReleaseSelector = new PreviousReleaseSelector();
        this.apkDownloader = new ApkDownloader(this.context);
        this.sha256SumsUrlResolver = new Sha256SumsUrlResolver();
        this.sha256SumsParser = new Sha256SumsParser();
        this.apkFileValidator = new ApkFileValidator();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static boolean isAutoCheckEnabled(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREFERENCE_KEY_AUTO_CHECK, false);
    }

    public static long getLastCheckTime(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getLong(PREFERENCE_KEY_LAST_CHECK_TIME, NO_CHECK_TIME);
    }

    public static void recordCheckTime(Context context, long epochMillis) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putLong(PREFERENCE_KEY_LAST_CHECK_TIME, epochMillis).apply();
    }

    public void checkForUpdate(CheckListener listener) {
        new Thread(() -> {
            try {
                String json = resolveLatestReleaseJson();
                ApkUpdateAvailability availability =
                    updatePlanner.plan(json, BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS);
                if (availability.isUpdateAvailable()) {
                    mainHandler.post(() -> {
                        recordCheckTime(context, System.currentTimeMillis());
                        listener.onUpdateAvailable(availability);
                    });
                } else {
                    mainHandler.post(() -> {
                        recordCheckTime(context, System.currentTimeMillis());
                        listener.onUpToDate(availability.getLatestVersionName());
                    });
                }
            } catch (GithubRateLimitedException rateLimitedException) {
                String message = messageOf(rateLimitedException);
                mainHandler.post(() -> listener.onCheckFailed(message, true));
            } catch (Exception exception) {
                String message = messageOf(exception);
                mainHandler.post(() -> listener.onCheckFailed(message, false));
            }
        }).start();
    }

    /**
     * Resolves the latest-release JSON, preferring the REST {@code releases/latest} endpoint. When
     * that endpoint answers with an exhausted rate limit, it falls back to the github.com Atom feed,
     * which is not subject to the 60-requests-per-hour unauthenticated REST limit, and synthesizes an
     * equivalent JSON document so the rest of the pipeline is unchanged. If the fallback cannot find a
     * release, the original rate-limit failure is propagated so the user still sees a rate-limit
     * message rather than a false "up to date".
     */
    private String resolveLatestReleaseJson() throws Exception {
        try {
            return releaseClient.fetchLatestReleaseJson(updateGuide.getReleasesLatestApiUrl());
        } catch (GithubRateLimitedException rateLimitedException) {
            String atomFeed = releaseClient.fetchReleasesAtomFeed(updateGuide.getReleasesAtomFeedUrl());
            String latestTagName = atomReleaseFeedParser.parseLatestTagName(atomFeed);
            if (latestTagName == null) {
                throw rateLimitedException;
            }
            return atomReleaseJsonSynthesizer.synthesizeReleaseJson(latestTagName);
        }
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.toString();
    }

    public void fetchPreviousBuilds(String currentVersionName, PreviousBuildsListener listener) {
        new Thread(() -> {
            try {
                String json = releaseClient.fetchReleasesListJson(updateGuide.getReleasesListApiUrl());
                java.util.List<ApkRelease> releases = releaseListParser.parseReleases(json);
                java.util.List<ApkRelease> previousBuilds =
                    previousReleaseSelector.selectOlderThan(releases, currentVersionName);
                mainHandler.post(() -> listener.onPreviousBuilds(previousBuilds));
            } catch (GithubRateLimitedException rateLimitedException) {
                String message = messageOf(rateLimitedException);
                mainHandler.post(() -> listener.onPreviousBuildsFailed(message, true));
            } catch (Exception exception) {
                String message = messageOf(exception);
                mainHandler.post(() -> listener.onPreviousBuildsFailed(message, false));
            }
        }).start();
    }

    public void downloadApk(String downloadUrl, String assetName, long expectedSizeBytes, DownloadListener listener) {
        new Thread(() -> {
            try {
                String expectedSha256 = resolveExpectedSha256(downloadUrl, assetName);
                File apkFile = apkDownloader.download(downloadUrl, sanitizeFileName(assetName));
                String invalidReason = apkFileValidator.validate(apkFile, expectedSha256, expectedSizeBytes);
                if (invalidReason != null) {
                    apkFile.delete();
                    mainHandler.post(() -> listener.onDownloadFailed(invalidReason));
                    return;
                }
                mainHandler.post(() -> listener.onDownloaded(apkFile));
            } catch (Exception exception) {
                String message = exception.getMessage() != null ? exception.getMessage() : exception.toString();
                mainHandler.post(() -> listener.onDownloadFailed(message));
            }
        }).start();
    }

    @Nullable
    private String resolveExpectedSha256(String downloadUrl, String assetName) {
        String sumsUrl = sha256SumsUrlResolver.resolveFromAssetDownloadUrl(downloadUrl);
        if (sumsUrl == null) {
            return null;
        }
        try {
            String sumsContent = releaseClient.fetchReleaseSha256Sums(sumsUrl);
            return sha256SumsParser.findExpectedSha256(sumsContent, assetName);
        } catch (Exception exception) {
            return null;
        }
    }

    private String sanitizeFileName(String assetName) {
        String name = assetName.replace('/', '_').replace('\\', '_');
        if (!name.endsWith(".apk")) {
            name = name + ".apk";
        }
        return name;
    }
}
