package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.termux.BuildConfig;
import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.List;

/**
 * Drives the in-app "revert to a previous build" flow: it fetches the older releases, lets the user
 * pick one, warns about the risk before reverting, downloads and validates that release's APK, and
 * installs it through a PackageInstaller session that requests a downgrade so the install is
 * not refused with "cannot be installed".
 */
public final class ApkRevertUiController {

    private static final String LOG_TAG = "ApkRevertUiController";

    private final Activity activity;
    private final ApkUpdateManager updateManager;
    private final ApkInstaller apkInstaller;
    private final ApkAbiAssetSelector abiAssetSelector;
    private final PackageInstallerSessionInstaller sessionInstaller;

    public ApkRevertUiController(Activity activity) {
        this(activity, new ApkUpdateManager(activity), new ApkInstaller(activity),
            new ApkAbiAssetSelector(), new PackageInstallerSessionInstaller(activity));
    }

    ApkRevertUiController(Activity activity, ApkUpdateManager updateManager, ApkInstaller apkInstaller,
                          ApkAbiAssetSelector abiAssetSelector,
                          PackageInstallerSessionInstaller sessionInstaller) {
        this.activity = activity;
        this.updateManager = updateManager;
        this.apkInstaller = apkInstaller;
        this.abiAssetSelector = abiAssetSelector;
        this.sessionInstaller = sessionInstaller;
    }

    public void startRevert() {
        Logger.showToast(activity, activity.getString(R.string.apk_revert_loading), false);
        updateManager.fetchPreviousBuilds(BuildConfig.VERSION_NAME,
            new ApkUpdateManager.PreviousBuildsListener() {
                @Override
                public void onPreviousBuilds(List<ApkRelease> previousBuilds) {
                    if (activity.isFinishing()) return;
                    if (previousBuilds.isEmpty()) {
                        Logger.showToast(activity, activity.getString(R.string.apk_revert_none_available), false);
                        return;
                    }
                    showVersionPicker(previousBuilds);
                }

                @Override
                public void onPreviousBuildsFailed(String message, boolean rateLimited) {
                    Logger.logError(LOG_TAG, "Fetching previous builds failed: " + message);
                    if (activity.isFinishing()) return;
                    Logger.showToast(activity, loadFailedMessage(message, rateLimited), true);
                }
            });
    }

    private String loadFailedMessage(String message, boolean rateLimited) {
        if (rateLimited) {
            return activity.getString(R.string.apk_update_check_rate_limited);
        }
        return activity.getString(R.string.apk_revert_load_failed, message);
    }

    private void showVersionPicker(@NonNull List<ApkRelease> previousBuilds) {
        CharSequence[] versionLabels = new CharSequence[previousBuilds.size()];
        for (int index = 0; index < previousBuilds.size(); index++) {
            versionLabels[index] = previousBuilds.get(index).getVersionName();
        }
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.apk_revert_dialog_title)
            .setItems(versionLabels, (dialog, which) -> confirmRevert(previousBuilds.get(which)))
            .setNegativeButton(R.string.apk_revert_cancel, null));
    }

    private void confirmRevert(@NonNull ApkRelease release) {
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.apk_revert_confirm_title, release.getVersionName()))
            .setMessage(R.string.apk_revert_confirm_message)
            .setPositiveButton(R.string.apk_revert_confirm_button, (dialog, which) -> beginDownloadAndInstall(release))
            .setNegativeButton(R.string.apk_revert_cancel, null));
    }

    private void beginDownloadAndInstall(@NonNull ApkRelease release) {
        if (!apkInstaller.canRequestPackageInstalls()) {
            Logger.showToast(activity,
                activity.getString(R.string.apk_revert_install_permission_required), true);
            Intent settingsIntent = apkInstaller.buildInstallUnknownAppsSettingsIntent();
            if (settingsIntent != null) {
                activity.startActivity(settingsIntent);
            }
            return;
        }

        ReleaseAsset asset = abiAssetSelector.selectForAbis(release.getAssets(), Build.SUPPORTED_ABIS);
        if (asset == null) {
            Logger.showToast(activity, activity.getString(R.string.apk_revert_no_matching_asset), true);
            return;
        }

        Context applicationContext = activity.getApplicationContext();
        Logger.showToast(activity, activity.getString(R.string.apk_revert_downloading), false);
        updateManager.downloadApk(asset.getDownloadUrl(), asset.getName(), asset.getSize(),
            new ApkUpdateManager.DownloadListener() {
                @Override
                public void onDownloaded(File apkFile) {
                    installDownloadedBuild(apkFile);
                }

                @Override
                public void onDownloadFailed(String message) {
                    Logger.logError(LOG_TAG, "Revert download failed: " + message);
                    Logger.showToast(applicationContext,
                        applicationContext.getString(R.string.apk_revert_download_failed, message), true);
                }
            });
    }

    private void installDownloadedBuild(File apkFile) {
        Context applicationContext = activity.getApplicationContext();
        sessionInstaller.install(apkFile, true, message -> {
            Logger.logError(LOG_TAG, "Revert install session failed: " + message);
            Logger.showToast(applicationContext,
                applicationContext.getString(R.string.apk_revert_install_failed, message), true);
        });
    }
}
