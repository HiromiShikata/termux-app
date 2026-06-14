package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

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

    public ApkUpdateUiController(Activity activity) {
        this.activity = activity;
        this.updateManager = new ApkUpdateManager(activity);
        this.apkInstaller = new ApkInstaller(activity);
        this.notificationPolicy = new ApkUpdateNotificationPolicy();
    }

    public void checkAndPrompt(boolean userInitiated) {
        if (userInitiated) {
            Logger.showToast(activity, activity.getString(R.string.apk_update_checking), false);
        }
        updateManager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                if (activity.isFinishing()) return;
                promptInstall(availability);
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                if (notificationPolicy.shouldNotifyUpToDate(userInitiated)) {
                    Logger.showToast(activity,
                        activity.getString(R.string.apk_update_up_to_date, latestVersionName), false);
                }
            }

            @Override
            public void onCheckFailed(String message) {
                Logger.logError(LOG_TAG, "APK update check failed: " + message);
                if (notificationPolicy.shouldNotifyCheckFailed(userInitiated)) {
                    Logger.showToast(activity,
                        activity.getString(R.string.apk_update_check_failed, message), true);
                }
            }
        });
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
            Intent settingsIntent = apkInstaller.buildInstallUnknownAppsSettingsIntent();
            if (settingsIntent != null) {
                activity.startActivity(settingsIntent);
            }
            return;
        }

        Logger.showToast(activity, activity.getString(R.string.apk_update_downloading), false);
        updateManager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    if (activity.isFinishing()) return;
                    activity.startActivity(apkInstaller.buildInstallIntent(apkFile));
                }

                @Override
                public void onDownloadFailed(String message) {
                    Logger.logError(LOG_TAG, "APK update download failed: " + message);
                    Logger.showToast(activity,
                        activity.getString(R.string.apk_update_download_failed, message), true);
                }
            });
    }
}
