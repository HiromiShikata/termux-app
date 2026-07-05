package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

import com.termux.BuildConfig;

import java.io.File;

public final class ApkUpdateManager {

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

    private final Context context;
    private final ApkUpdateGuide updateGuide;
    private final GithubReleaseClient releaseClient;
    private final GithubAtomReleaseFeedParser atomReleaseFeedParser;
    private final AtomReleaseJsonSynthesizer atomReleaseJsonSynthesizer;
    private final ApkUpdatePlanner updatePlanner;
    private final ApkDownloader apkDownloader;
    private final Handler mainHandler;

    public ApkUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.updateGuide = new ApkUpdateGuide();
        this.releaseClient = new GithubReleaseClient();
        this.atomReleaseFeedParser = new GithubAtomReleaseFeedParser();
        this.atomReleaseJsonSynthesizer =
            new AtomReleaseJsonSynthesizer(updateGuide.getReleasesOwner(), updateGuide.getReleasesRepo());
        this.updatePlanner = new ApkUpdatePlanner();
        this.apkDownloader = new ApkDownloader(this.context);
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

    public void downloadApk(String downloadUrl, String assetName, DownloadListener listener) {
        new Thread(() -> {
            try {
                File apkFile = apkDownloader.download(downloadUrl, sanitizeFileName(assetName));
                mainHandler.post(() -> listener.onDownloaded(apkFile));
            } catch (Exception exception) {
                String message = exception.getMessage() != null ? exception.getMessage() : exception.toString();
                mainHandler.post(() -> listener.onDownloadFailed(message));
            }
        }).start();
    }

    private String sanitizeFileName(String assetName) {
        String name = assetName.replace('/', '_').replace('\\', '_');
        if (!name.endsWith(".apk")) {
            name = name + ".apk";
        }
        return name;
    }
}
