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
        this(activity, new ApkUpdateManager(activity), new ApkInstaller(activity));
    }

    UpdateTagUpdateRunner(@NonNull Activity activity, @NonNull ApkUpdateManager updateManager,
                          @NonNull ApkInstaller apkInstaller) {
        this.activity = activity;
        this.updateManager = updateManager;
        this.apkInstaller = apkInstaller;
    }

    @Override
    public void onUpdateRequested(String reason) {
        if (updateInProgress) return;
        updateInProgress = true;

        Logger.showToast(activity, activity.getString(R.string.apk_update_checking), false);
        updateManager.checkForUpdate(new ApkUpdateManager.CheckListener() {
            @Override
            public void onUpdateAvailable(ApkUpdateAvailability availability) {
                promptUpdate(availability, reason);
            }

            @Override
            public void onUpToDate(String latestVersionName) {
                updateInProgress = false;
                Logger.showToast(activity,
                    activity.getString(R.string.apk_update_up_to_date, latestVersionName), false);
            }

            @Override
            public void onCheckFailed(String message, boolean rateLimited) {
                updateInProgress = false;
                Logger.logError(LOG_TAG, "Update tag check failed: " + message);
                String toastMessage = rateLimited
                    ? activity.getString(R.string.apk_update_check_rate_limited)
                    : activity.getString(R.string.apk_update_check_failed, message);
                Logger.showToast(activity, toastMessage, true);
            }
        });
    }

    private void promptUpdate(ApkUpdateAvailability availability, String reason) {
        updateInProgress = false;
        if (activity.isFinishing()) {
            downloadAndInstall(availability);
            return;
        }
        AlertDialog updateDialog = DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.apk_update_dialog_title)
            .setMessage(activity.getString(R.string.update_tag_update_dialog_message,
                availability.getLatestVersionName(), reason))
            .setPositiveButton(R.string.apk_update_dialog_install,
                (dialog, which) -> downloadAndInstall(availability))
            .setNegativeButton(R.string.apk_update_dialog_cancel, null));
        UpdateDialogContentTextSizeReducer.reduceContentTextToHalf(updateDialog);
    }

    private void downloadAndInstall(ApkUpdateAvailability availability) {
        Context applicationContext = activity.getApplicationContext();
        Logger.showToast(activity, activity.getString(R.string.apk_update_downloading), false);
        updateManager.downloadApk(availability.getDownloadUrl(), availability.getAssetName(),
            availability.getExpectedSizeBytes(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    launchInstall(apkFile);
                }

                @Override
                public void onDownloadFailed(String message) {
                    Logger.logError(LOG_TAG, "Update tag download failed: " + message);
                    Logger.showToast(applicationContext,
                        applicationContext.getString(R.string.apk_update_download_failed, message), true);
                }
            });
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
