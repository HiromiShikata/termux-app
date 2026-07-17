package com.termux.app.apkupdate;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UpdateTagUpdateRunnerTest {

    private static final ApkUpdateAvailability AVAILABILITY =
        ApkUpdateAvailability.available("1.2.3", "https://example.com/app.apk", "app.apk", 1000L);

    private static final class FakeUpdateManager extends ApkUpdateManager {
        boolean reportAvailable = true;
        int downloadCount;

        FakeUpdateManager(Activity activity) {
            super(activity);
        }

        @Override
        public void checkForUpdate(CheckListener listener) {
            if (reportAvailable) {
                listener.onUpdateAvailable(AVAILABILITY);
            } else {
                listener.onUpToDate("1.2.3");
            }
        }

        @Override
        public void downloadApk(String downloadUrl, String assetName, long expectedSizeBytes,
                                DownloadListener listener) {
            downloadCount++;
            listener.onDownloaded(new File("/dev/null"));
        }
    }

    private static final class FakeApkInstaller extends ApkInstaller {
        final List<File> installedFiles = new ArrayList<>();

        FakeApkInstaller(Activity activity) {
            super(activity);
        }

        @Override
        public boolean canRequestPackageInstalls() {
            return true;
        }

        @Override
        public Intent buildInstallIntent(File apkFile) {
            installedFiles.add(apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
    }

    private Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).create().start().resume().get();
    }

    @Test
    public void showsSingleUpdateDialogBeforeDownloading() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(activity, manager, installer);

        runner.onUpdateRequested("security fix");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        Assert.assertTrue(dialog.isShowing());
        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertEquals(activity.getString(R.string.apk_update_dialog_install),
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        Assert.assertEquals(activity.getString(R.string.apk_update_dialog_cancel),
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
    }

    @Test
    public void tappingUpdateStartsDownloadAndInstall() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(activity, manager, installer);

        runner.onUpdateRequested("security fix");
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(1, manager.downloadCount);
        Assert.assertEquals(1, installer.installedFiles.size());
    }

    @Test
    public void tappingLaterDeclinesWithoutDownloading() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(activity, manager, installer);

        runner.onUpdateRequested("security fix");
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertTrue(installer.installedFiles.isEmpty());
    }

    @Test
    public void decliningThenNewTagPromptsAgain() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(activity, manager, installer);

        runner.onUpdateRequested("first reason");
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        runner.onUpdateRequested("second reason");

        AlertDialog secondDialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(secondDialog);
        Assert.assertTrue(secondDialog.isShowing());
        Assert.assertEquals(0, manager.downloadCount);
    }
}
