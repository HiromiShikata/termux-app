package com.termux.app.apkupdate;

import android.content.Context;

import androidx.annotation.NonNull;
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
                downloadThenNotify(context, manager, availability, latch, outcome);
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

    private void downloadThenNotify(Context context, ApkUpdateManager manager, ApkUpdateAvailability availability,
                                    CountDownLatch latch, AtomicReference<Result> outcome) {
        manager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            availability.getExpectedSizeBytes(), new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    notifyWithNewerReleases(context, manager, availability, apkFile, latch, outcome);
                }

                @Override
                public void onDownloadFailed(String message) {
                    ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
                    Logger.logError(LOG_TAG, "APK update background download failed: " + message);
                    latch.countDown();
                }
            });
    }

    private static void notifyWithNewerReleases(Context context, ApkUpdateManager manager,
                                                ApkUpdateAvailability availability, File apkFile,
                                                CountDownLatch latch, AtomicReference<Result> outcome) {
        manager.fetchNewerReleases(BuildConfig.VERSION_NAME, new ApkUpdateManager.NewerReleasesListener() {
            @Override
            public void onNewerReleases(List<ApkRelease> newerReleases) {
                ApkUpdateAvailability downloadedAvailability =
                    availability.withDownloadedFilePath(apkFile.getAbsolutePath());
                ApkUpdatePendingState.Store store = new SharedPreferencesApkUpdatePendingStore(context);
                new ApkUpdatePendingState(store).save(downloadedAvailability);
                if (!new ApkInstaller(context).canRequestPackageInstalls()) {
                    new ApkUpdateInstallResumeRequest(store).saveResumeRequest(downloadedAvailability);
                }
                ApkUpdateNotifier.notifyDownloadedUpdate(context, availability.getLatestVersionName(), apkFile,
                    UpdateNotificationTextComposer.composeBigText(newerReleases));
                ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
                latch.countDown();
            }

            @Override
            public void onNewerReleasesFailed(String message, boolean rateLimited) {
                Logger.logError(LOG_TAG, "APK update release list fetch failed: " + message);
                ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
                outcome.set(Result.retry());
                latch.countDown();
            }
        });
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
