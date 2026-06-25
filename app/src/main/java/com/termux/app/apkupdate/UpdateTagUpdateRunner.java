package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;

import java.io.File;

public final class UpdateTagUpdateRunner implements UpdateTagUpdateController.ReasonTrigger {

    private static final String LOG_TAG = "UpdateTagUpdateRunner";

    private final Activity activity;
    private final ApkUpdateManager updateManager;
    private final ApkInstaller apkInstaller;

    private boolean updateInProgress;

    public UpdateTagUpdateRunner(@NonNull Activity activity) {
        this.activity = activity;
        this.updateManager = new ApkUpdateManager(activity);
        this.apkInstaller = new ApkInstaller(activity);
    }

    @Override
    public void onUpdateRequested(String reason) {
        if (updateInProgress) return;
        updateInProgress = true;

        Logger.showToast(activity, activity.getString(R.string.apk_update_checking), false);
        updateManager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                downloadAndPrompt(availability, reason);
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                updateInProgress = false;
                Logger.showToast(activity,
                    activity.getString(R.string.apk_update_up_to_date, latestVersionName), false);
            }

            @Override
            public void onCheckFailed(String message) {
                updateInProgress = false;
                Logger.logError(LOG_TAG, "Update tag check failed: " + message);
                Logger.showToast(activity,
                    activity.getString(R.string.apk_update_check_failed, message), true);
            }
        });
    }

    private void downloadAndPrompt(ApkUpdateAvailability availability, String reason) {
        Context applicationContext = activity.getApplicationContext();
        Logger.showToast(activity, activity.getString(R.string.apk_update_downloading), false);
        updateManager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    updateInProgress = false;
                    promptInstall(apkFile, availability.getLatestVersionName(), reason);
                }

                @Override
                public void onDownloadFailed(String message) {
                    updateInProgress = false;
                    Logger.logError(LOG_TAG, "Update tag download failed: " + message);
                    Logger.showToast(applicationContext,
                        applicationContext.getString(R.string.apk_update_download_failed, message), true);
                }
            });
    }

    private void promptInstall(File apkFile, String latestVersionName, String reason) {
        if (activity.isFinishing()) {
            launchInstall(apkFile);
            return;
        }
        AlertDialog installDialog = DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.update_tag_install_dialog_title)
            .setMessage(activity.getString(R.string.update_tag_install_dialog_message, latestVersionName, reason))
            .setPositiveButton(R.string.apk_update_dialog_install,
                (dialog, which) -> launchInstall(apkFile))
            .setNegativeButton(R.string.apk_update_dialog_cancel, null));
        UpdateDialogContentTextSizeReducer.reduceContentTextToHalf(installDialog);
    }

    private void launchInstall(File apkFile) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            Logger.showToast(activity,
                activity.getString(R.string.apk_update_install_permission_required), true);
            Intent settingsIntent = apkInstaller.buildInstallUnknownAppsSettingsIntent();
            if (settingsIntent != null) {
                activity.startActivity(settingsIntent);
            }
            return;
        }
        activity.getApplicationContext().startActivity(apkInstaller.buildInstallIntent(apkFile));
    }
}
