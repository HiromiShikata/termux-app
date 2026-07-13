package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;

import java.io.File;

public final class ApkUpdateUiController {

    private static final String LOG_TAG = "ApkUpdateUiController";

    private final Activity activity;
    private final ApkUpdateManager updateManager;
    private final ApkInstaller apkInstaller;
    private final ApkUpdateNotificationPolicy notificationPolicy;
    private final ApkUpdatePendingState pendingState;
    private final ApkUpdateInstallResumeRequest pendingInstallResume;
    private final ApkUpdateCachedFileResolver cachedFileResolver;

    public ApkUpdateUiController(Activity activity) {
        this.activity = activity;
        this.updateManager = new ApkUpdateManager(activity);
        this.apkInstaller = new ApkInstaller(activity);
        this.notificationPolicy = new ApkUpdateNotificationPolicy();
        ApkUpdatePendingState.Store store = new SharedPreferencesApkUpdatePendingStore(activity);
        this.pendingState = new ApkUpdatePendingState(store);
        this.pendingInstallResume = new ApkUpdateInstallResumeRequest(store);
        this.cachedFileResolver = new ApkUpdateCachedFileResolver();
    }

    public void checkAndPrompt(boolean userInitiated) {
        if (userInitiated) {
            Logger.showToast(activity, activity.getString(R.string.apk_update_checking), false);
        }
        updateManager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                pendingState.save(availability);
                if (activity.isFinishing()) return;
                if (userInitiated) {
                    promptInstall(availability);
                }
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                pendingState.clear();
                if (notificationPolicy.shouldNotifyUpToDate(userInitiated)) {
                    Logger.showToast(activity,
                        activity.getString(R.string.apk_update_up_to_date, latestVersionName), false);
                }
            }

            @Override
            public void onCheckFailed(String message, boolean rateLimited) {
                Logger.logError(LOG_TAG, "APK update check failed: " + message);
                if (notificationPolicy.shouldNotifyCheckFailed(userInitiated, rateLimited)) {
                    Logger.showToast(activity, checkFailedMessage(message, rateLimited), true);
                }
            }
        });
    }

    private String checkFailedMessage(String message, boolean rateLimited) {
        if (rateLimited) {
            return activity.getString(R.string.apk_update_check_rate_limited);
        }
        return activity.getString(R.string.apk_update_check_failed, message);
    }

    public void checkAndShowFloatingIndicator(ApkUpdateFloatingIndicatorController.IndicatorView indicatorView) {
        if (pendingState.isInstallLaunchSuppressed(BuildConfig.VERSION_NAME, System.currentTimeMillis())) {
            return;
        }
        ApkUpdateFloatingIndicatorController indicatorController = newIndicatorController(indicatorView);
        updateManager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                if (pendingState.isInstallLaunchSuppressed(BuildConfig.VERSION_NAME, System.currentTimeMillis())) {
                    return;
                }
                pendingState.save(availability);
                preDownloadThenShowIndicator(availability, indicatorController);
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                pendingState.clear();
                indicatorController.onUpToDate();
            }

            @Override
            public void onCheckFailed(String message, boolean rateLimited) {
                Logger.logError(LOG_TAG, "APK update check failed: " + message);
                if (notificationPolicy.shouldNotifyCheckFailed(false, rateLimited)) {
                    Logger.showToast(activity, checkFailedMessage(message, rateLimited), true);
                }
            }
        });
    }

    public void showPendingIndicatorIfAny(ApkUpdateFloatingIndicatorController.IndicatorView indicatorView) {
        if (pendingState.isInstallLaunchSuppressed(BuildConfig.VERSION_NAME, System.currentTimeMillis())) {
            indicatorView.hide();
            return;
        }
        ApkUpdateAvailability availability = pendingState.loadIfNewerThanInstalled(BuildConfig.VERSION_NAME);
        if (availability == null) {
            indicatorView.hide();
            return;
        }
        newIndicatorController(indicatorView).onUpdateAvailable(availability);
    }

    private void preDownloadThenShowIndicator(ApkUpdateAvailability availability,
                                              ApkUpdateFloatingIndicatorController indicatorController) {
        Logger.logInfo(LOG_TAG, "Pre-downloading APK update before showing indicator");
        updateManager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            availability.getExpectedSizeBytes(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    ApkUpdateAvailability downloadedAvailability =
                        availability.withDownloadedFilePath(apkFile.getAbsolutePath());
                    pendingState.save(downloadedAvailability);
                    if (activity.isFinishing()) return;
                    indicatorController.onUpdateAvailable(downloadedAvailability);
                }

                @Override
                public void onDownloadFailed(String message) {
                    Logger.logError(LOG_TAG, "APK update pre-download failed: " + message);
                    ApkUpdateAvailability withoutDownloadedFile = availability.withDownloadedFilePath(null);
                    pendingState.save(withoutDownloadedFile);
                    if (activity.isFinishing()) return;
                    indicatorController.onUpdateAvailable(withoutDownloadedFile);
                }
            });
    }

    private ApkUpdateFloatingIndicatorController newIndicatorController(
        ApkUpdateFloatingIndicatorController.IndicatorView indicatorView) {
        return new ApkUpdateFloatingIndicatorController(indicatorView, this::startDownloadAndInstall);
    }

    private void promptInstall(ApkUpdateAvailability availability) {
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.apk_update_dialog_title)
            .setMessage(activity.getString(R.string.apk_update_dialog_message, availability.getLatestVersionName()))
            .setPositiveButton(R.string.apk_update_dialog_install,
                (dialog, which) -> startDownloadAndInstall(availability))
            .setNegativeButton(R.string.apk_update_dialog_cancel, null));
    }

    private void startDownloadAndInstall(ApkUpdateAvailability availability) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            Logger.showToast(activity,
                activity.getString(R.string.apk_update_install_permission_required), true);
            pendingInstallResume.saveResumeRequest(availability);
            Intent settingsIntent = apkInstaller.buildInstallUnknownAppsSettingsIntent();
            if (settingsIntent != null) {
                activity.startActivity(settingsIntent);
            }
            return;
        }

        File cachedApkFile = cachedFileResolver.resolveExistingFile(availability);
        if (cachedApkFile != null) {
            pendingState.clear();
            launchInstall(availability, cachedApkFile);
            return;
        }

        Context applicationContext = activity.getApplicationContext();
        Logger.showToast(activity, activity.getString(R.string.apk_update_downloading), false);
        updateManager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            availability.getExpectedSizeBytes(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    pendingState.clear();
                    launchInstall(availability, apkFile);
                }

                @Override
                public void onDownloadFailed(String message) {
                    Logger.logError(LOG_TAG, "APK update download failed: " + message);
                    Logger.showToast(applicationContext,
                        applicationContext.getString(R.string.apk_update_download_failed, message), true);
                }
            });
    }

    public void resumePendingInstallIfPermissionGranted() {
        ApkUpdateAvailability availability = pendingInstallResume.loadResumeRequest();
        if (availability == null) {
            return;
        }
        if (!apkInstaller.canRequestPackageInstalls()) {
            return;
        }
        pendingInstallResume.clearResumeRequest();
        startDownloadAndInstall(availability);
    }

    private void launchInstall(ApkUpdateAvailability availability, File apkFile) {
        pendingState.markInstallLaunched(availability.getLatestVersionName(), System.currentTimeMillis());
        pendingInstallResume.clearResumeRequest();
        activity.getApplicationContext().startActivity(apkInstaller.buildInstallIntent(apkFile));
    }
}
