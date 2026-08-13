package com.termux.app.apkupdate;

import android.app.Activity;
import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UpdateTagUpdateRunnerTest {

    private static final ApkUpdateAvailability AVAILABILITY =
        ApkUpdateAvailability.available("1.2.3", "https://example.com/app.apk", "app.apk", 0L);

    private static final class FakeUpdateManager extends ApkUpdateManager {
        boolean reportAvailable = true;
        int downloadCount;
        File downloadedFile;

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
            listener.onDownloaded(downloadedFile);
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

    private static final class RecordingIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        final List<String> shownVersions = new ArrayList<>();
        int hideCount;

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
        }

        @Override
        public void hide() {
            hideCount++;
        }
    }

    @Before
    public void resetDownloadGuard() {
        ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
    }

    private Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).create().start().resume().get();
    }

    private File newValidApkFile() throws IOException {
        File file = File.createTempFile("termux-update", ".apk");
        byte[] content = new byte[(int) (1024L * 1024L + 16L)];
        content[0] = 0x50;
        content[1] = 0x4B;
        content[2] = 0x03;
        content[3] = 0x04;
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(content);
        }
        file.deleteOnExit();
        return file;
    }

    @Test
    public void updateTagAutoDownloadsFirstAndOnlyThenOffersTheDialog() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.downloadedFile = newValidApkFile();
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(
            new ApkUpdateUiController(activity, manager, installer), indicatorView);

        runner.onUpdateRequested("security fix");

        Assert.assertNotNull("the update tag must offer the dialog once the APK is ready",
            ShadowAlertDialog.getLatestAlertDialog());
        Assert.assertEquals("the update tag must auto-download the APK", 1, manager.downloadCount);
        Assert.assertEquals("the floating install button must surface directly", 1, indicatorView.shownVersions.size());
        Assert.assertEquals("1.2.3", indicatorView.shownVersions.get(0));
        Assert.assertTrue("nothing is installed until the button is tapped", installer.installedFiles.isEmpty());
    }

    @Test
    public void updateTagWhenUpToDateDownloadsNothingAndHidesButton() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.reportAvailable = false;
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        UpdateTagUpdateRunner runner = new UpdateTagUpdateRunner(
            new ApkUpdateUiController(activity, manager, installer), indicatorView);

        runner.onUpdateRequested("security fix");

        Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog());
        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertEquals(1, indicatorView.hideCount);
    }
}
