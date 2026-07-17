package com.termux.app.apkupdate;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Looper;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ApkRevertUiControllerTest {

    private static ApkRelease releaseWithUniversalAsset(String versionName) {
        List<ReleaseAsset> assets = Collections.singletonList(
            new ReleaseAsset("termux-app_universal.apk",
                "https://example.com/" + versionName + "_universal.apk", 4000000L));
        return new ApkRelease(versionName, "v" + versionName, assets);
    }

    private static final class FakeUpdateManager extends ApkUpdateManager {
        List<ApkRelease> previousBuilds = new ArrayList<>();
        boolean failLoad;
        int downloadCount;
        String lastDownloadUrl;

        FakeUpdateManager(Activity activity) {
            super(activity);
        }

        @Override
        public void fetchPreviousBuilds(String currentVersionName, PreviousBuildsListener listener) {
            if (failLoad) {
                listener.onPreviousBuildsFailed("boom", false);
            } else {
                listener.onPreviousBuilds(previousBuilds);
            }
        }

        @Override
        public void downloadApk(String downloadUrl, String assetName, long expectedSizeBytes,
                                DownloadListener listener) {
            downloadCount++;
            lastDownloadUrl = downloadUrl;
            listener.onDownloaded(new File("/dev/null"));
        }
    }

    private static final class FakeApkInstaller extends ApkInstaller {
        FakeApkInstaller(Activity activity) {
            super(activity);
        }

        @Override
        public boolean canRequestPackageInstalls() {
            return true;
        }
    }

    private static final class FakeSessionInstaller extends PackageInstallerSessionInstaller {
        final List<File> installedFiles = new ArrayList<>();
        final List<Boolean> downgradeRequests = new ArrayList<>();

        FakeSessionInstaller(Activity activity) {
            super(activity);
        }

        @Override
        public void install(File apkFile, boolean requestDowngrade, InstallListener listener) {
            installedFiles.add(apkFile);
            downgradeRequests.add(requestDowngrade);
        }
    }

    private Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).create().start().resume().get();
    }

    private ApkRevertUiController newController(Activity activity, FakeUpdateManager manager,
                                               FakeSessionInstaller installer) {
        return new ApkRevertUiController(activity, manager, new FakeApkInstaller(activity),
            new ApkAbiAssetSelector(), installer);
    }

    @Test
    public void showsVersionPickerWithPreviousBuilds() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.previousBuilds = Arrays.asList(
            releaseWithUniversalAsset("0.118.0"), releaseWithUniversalAsset("0.117.0"));
        FakeSessionInstaller installer = new FakeSessionInstaller(activity);

        newController(activity, manager, installer).startRevert();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        Assert.assertTrue(dialog.isShowing());
        Assert.assertTrue(installer.installedFiles.isEmpty());
    }

    @Test
    public void selectingVersionShowsDataLossWarningBeforeInstalling() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.previousBuilds = Collections.singletonList(releaseWithUniversalAsset("0.118.0"));
        FakeSessionInstaller installer = new FakeSessionInstaller(activity);

        newController(activity, manager, installer).startRevert();
        shadowOf(ShadowAlertDialog.getLatestAlertDialog()).clickOnItem(0);

        AlertDialog confirmDialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertEquals(activity.getString(R.string.apk_revert_confirm_message),
            shadowOf(confirmDialog).getMessage());
        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertTrue(installer.installedFiles.isEmpty());
    }

    @Test
    public void confirmingRevertDownloadsThenInstallsWithDowngradeRequested() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.previousBuilds = Collections.singletonList(releaseWithUniversalAsset("0.118.0"));
        FakeSessionInstaller installer = new FakeSessionInstaller(activity);

        newController(activity, manager, installer).startRevert();
        shadowOf(ShadowAlertDialog.getLatestAlertDialog()).clickOnItem(0);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(1, manager.downloadCount);
        Assert.assertEquals("https://example.com/0.118.0_universal.apk", manager.lastDownloadUrl);
        Assert.assertEquals(1, installer.installedFiles.size());
        Assert.assertEquals(Boolean.TRUE, installer.downgradeRequests.get(0));
    }

    @Test
    public void cancellingConfirmationDoesNotDownloadOrInstall() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.previousBuilds = Collections.singletonList(releaseWithUniversalAsset("0.118.0"));
        FakeSessionInstaller installer = new FakeSessionInstaller(activity);

        newController(activity, manager, installer).startRevert();
        shadowOf(ShadowAlertDialog.getLatestAlertDialog()).clickOnItem(0);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        Assert.assertEquals(0, manager.downloadCount);
        Assert.assertTrue(installer.installedFiles.isEmpty());
    }

    @Test
    public void showsMessageWhenNoPreviousBuildsAvailable() {
        Activity activity = newActivity();
        FakeUpdateManager manager = new FakeUpdateManager(activity);
        manager.previousBuilds = Collections.emptyList();
        FakeSessionInstaller installer = new FakeSessionInstaller(activity);

        newController(activity, manager, installer).startRevert();
        shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(activity.getString(R.string.apk_revert_none_available),
            ShadowToast.getTextOfLatestToast());
        Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog());
        Assert.assertTrue(installer.installedFiles.isEmpty());
    }
}
