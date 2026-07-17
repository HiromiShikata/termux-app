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
public class ApkUpdateUiControllerTest {

    private static final ApkUpdateAvailability AVAILABILITY =
        ApkUpdateAvailability.available("1.2.3", "https://example.com/app.apk", "app.apk", 0L);

    private static final class FakeUpdateManager extends ApkUpdateManager {
        boolean reportAvailable = true;
        boolean autoCompleteDownload = true;
        int checkCount;
        int downloadCount;
        File downloadedFile;
        DownloadListener pendingDownloadListener;

        FakeUpdateManager(Activity activity) {
            super(activity);
        }

        @Override
        public void checkForUpdate(CheckListener listener) {
            checkCount++;
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
            if (autoCompleteDownload) {
                listener.onDownloaded(downloadedFile);
            } else {
                pendingDownloadListener = listener;
            }
        }

        void completePendingDownload() {
            DownloadListener listener = pendingDownloadListener;
            pendingDownloadListener = null;
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
        Runnable lastTapAction;
        int hideCount;

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
            lastTapAction = onTapped;
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
    public void checkAndShowFloatingIndicatorAutoDownloadsAndSurfacesButtonWithoutDialog() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.downloadedFile = newValidApkFile();
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(indicatorView);

        Assert.assertNull("no blocking dialog must be shown", ShadowAlertDialog.getLatestAlertDialog());
        Assert.assertEquals("the update APK must auto-download without any tap", 1, manager.downloadCount);
        Assert.assertEquals("the floating install button must surface directly", 1, indicatorView.shownVersions.size());
        Assert.assertEquals("1.2.3", indicatorView.shownVersions.get(0));
        Assert.assertTrue("nothing must be installed until the button is tapped", installer.installedFiles.isEmpty());
    }

    @Test
    public void deferringKeepsTheDownloadedApkReadyForALaterInstall() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.downloadedFile = newValidApkFile();
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());

        RecordingIndicatorView reopenedIndicatorView = new RecordingIndicatorView();
        controller.showPendingIndicatorIfAny(reopenedIndicatorView);

        Assert.assertEquals("the downloaded APK is kept ready so the button re-surfaces without re-downloading",
            1, reopenedIndicatorView.shownVersions.size());
        Assert.assertEquals("deferring the install must never trigger a re-download", 1, manager.downloadCount);
        Assert.assertTrue("deferring must never install", installer.installedFiles.isEmpty());
    }

    @Test
    public void tappingTheButtonInstallsTheKeptApkWithoutReDownloading() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        File cachedApk = newValidApkFile();
        manager.downloadedFile = cachedApk;
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(indicatorView);
        Assert.assertNotNull(indicatorView.lastTapAction);
        indicatorView.lastTapAction.run();

        Assert.assertEquals("tapping installs the update", 1, installer.installedFiles.size());
        Assert.assertEquals(cachedApk.getAbsolutePath(), installer.installedFiles.get(0).getAbsolutePath());
        Assert.assertEquals("the kept APK is reused, not downloaded again", 1, manager.downloadCount);
    }

    @Test
    public void userInitiatedSettingsCheckAutoDownloadsWithoutBlockingDialog() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.downloadedFile = newValidApkFile();
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndPrompt(true);

        Assert.assertNull("the settings check must not force a blocking dialog", ShadowAlertDialog.getLatestAlertDialog());
        Assert.assertEquals("the settings check must auto-download in the background", 1, manager.downloadCount);
        Assert.assertTrue("the settings check must not install without a tap", installer.installedFiles.isEmpty());
    }

    @Test
    public void concurrentUpdateEventsBeforeDownloadFinishesTriggerOnlyOneDownload() throws IOException {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.downloadedFile = newValidApkFile();
        manager.autoCompleteDownload = false;
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());
        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());

        Assert.assertEquals("a download already in progress must not be duplicated by a rapid second event",
            1, manager.downloadCount);

        manager.completePendingDownload();
        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());

        Assert.assertEquals("once the in-progress download completes the guard is cleared so a later event may download",
            2, manager.downloadCount);
    }

    @Test
    public void upToDateCheckDownloadsNothingAndHidesTheButton() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.reportAvailable = false;
        FakeApkInstaller installer = new FakeApkInstaller(activity);
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(indicatorView);

        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertTrue(indicatorView.shownVersions.isEmpty());
        Assert.assertEquals(1, indicatorView.hideCount);
    }
}
