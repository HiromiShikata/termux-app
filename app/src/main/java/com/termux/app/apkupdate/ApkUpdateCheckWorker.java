package com.termux.app.apkupdate;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.termux.BuildConfig;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ApkUpdateCheckWorker extends Worker {

    public static final String WORK_NAME = "ApkUpdatePeriodicCheck";
    private static final String LOG_TAG = "ApkUpdateCheckWorker";
    private static final long INTERVAL_HOURS = 6L;
    private static final long OUTCOME_TIMEOUT_MINUTES = 10L;

    public ApkUpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        ApkUpdateManager manager = new ApkUpdateManager(context);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Result> outcome = new AtomicReference<>(Result.success());
        manager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                if (!ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.compareAndSet(false, true)) {
                    Logger.logInfo(LOG_TAG, "An APK update is already being prepared; skipping background download");
                    latch.countDown();
                    return;
                }
                new Thread(() -> {
                    try {
                        ApkUpdateBackgroundPreparation.Outcome preparationOutcome =
                            prepare(context, manager, availability);
                        outcome.set(resultOf(preparationOutcome));
                    } finally {
                        ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
                        latch.countDown();
                    }
                }).start();
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(String message, boolean rateLimited) {
                Logger.logError(LOG_TAG, "APK update background check failed: " + message);
                latch.countDown();
            }
        });
        try {
            if (!latch.await(OUTCOME_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                Logger.logError(LOG_TAG, "APK update background check did not finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }
        return outcome.get();
    }

    private static ApkUpdateBackgroundPreparation.Outcome prepare(Context context, ApkUpdateManager manager,
                                                                  ApkUpdateAvailability availability) {
        ApkUpdateBackgroundPreparation preparation = new ApkUpdateBackgroundPreparation(
            BuildConfig.VERSION_NAME,
            cachedAvailability -> findValidatedCachedApk(context, cachedAvailability),
            downloadAvailability -> downloadBlocking(manager, downloadAvailability),
            () -> fetchReleasesBlocking(manager),
            (notifiedAvailability, apkFile, changesText) ->
                saveAndNotify(context, notifiedAvailability, apkFile, changesText));
        return preparation.run(availability);
    }

    private static Result resultOf(ApkUpdateBackgroundPreparation.Outcome outcome) {
        return outcome.requestsRetry()
            ? Result.retry()
            : Result.success();
    }

    @Nullable
    private static File findValidatedCachedApk(Context context, ApkUpdateAvailability availability) {
        ApkUpdateAvailability pendingAvailability =
            new ApkUpdatePendingState(new SharedPreferencesApkUpdatePendingStore(context)).load();
        if (pendingAvailability == null || !isSameReleaseAsset(pendingAvailability, availability)) {
            return null;
        }
        return new ApkUpdateCachedFileResolver().resolveExistingFile(pendingAvailability);
    }

    private static boolean isSameReleaseAsset(ApkUpdateAvailability first, ApkUpdateAvailability second) {
        return first.getLatestVersionName().equals(second.getLatestVersionName())
            && first.getDownloadUrl().equals(second.getDownloadUrl())
            && first.getAssetName().equals(second.getAssetName());
    }

    private static File downloadBlocking(ApkUpdateManager manager, ApkUpdateAvailability availability)
            throws IOException {
        CountDownLatch downloadLatch = new CountDownLatch(1);
        AtomicReference<File> downloadedFile = new AtomicReference<>();
        AtomicReference<String> failureMessage = new AtomicReference<>();
        manager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            availability.getExpectedSizeBytes(), new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    downloadedFile.set(apkFile);
                    downloadLatch.countDown();
                }

                @Override
                public void onDownloadFailed(String message) {
                    failureMessage.set(message);
                    downloadLatch.countDown();
                }
            });
        try {
            if (!downloadLatch.await(OUTCOME_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                throw failDownload("download did not finish in time");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failDownload("download was interrupted");
        }
        if (failureMessage.get() != null) {
            throw failDownload(failureMessage.get());
        }
        return downloadedFile.get();
    }

    private static IOException failDownload(String message) {
        Logger.logError(LOG_TAG, "APK update background download failed: " + message);
        return new IOException(message);
    }

    private static List<ApkRelease> fetchReleasesBlocking(ApkUpdateManager manager) throws IOException {
        try {
            return manager.fetchReleasesNewestFirstPaginated(BuildConfig.VERSION_NAME);
        } catch (IOException exception) {
            Logger.logError(LOG_TAG, "APK update release list fetch failed: " + exception.getMessage());
            throw exception;
        }
    }

    private static void saveAndNotify(Context context, ApkUpdateAvailability availability, File apkFile,
                                      String changesText) {
        ApkUpdateAvailability downloadedAvailability = availability.withDownloadedFilePath(apkFile.getAbsolutePath());
        ApkUpdatePendingState.Store store = new SharedPreferencesApkUpdatePendingStore(context);
        new ApkUpdatePendingState(store).save(downloadedAvailability);
        if (!new ApkInstaller(context).canRequestPackageInstalls()) {
            new ApkUpdateInstallResumeRequest(store).saveResumeRequest(downloadedAvailability);
        }
        ApkUpdateNotifier.notifyDownloadedUpdate(context, availability.getLatestVersionName(), apkFile, changesText);
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            ApkUpdateCheckWorker.class, INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest);
    }
}
