package com.termux.app.apkupdate;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;

import com.termux.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateIndicatorTapAlwaysRespondsTest {

    private static final ApkUpdateAvailability AVAILABILITY =
        ApkUpdateAvailability.available("1.2.3", "https://example.com/app.apk", "app.apk", 0L);

    private static final class DownloadNeverCompletingUpdateManager extends ApkUpdateManager {
        int downloadCount;

        DownloadNeverCompletingUpdateManager(Activity activity) {
            super(activity);
        }

        @Override
        public void checkForUpdate(CheckListener listener) {
            listener.onUpdateAvailable(AVAILABILITY);
        }

        @Override
        public void downloadApk(String downloadUrl, String assetName, long expectedSizeBytes,
                                DownloadListener listener) {
            downloadCount++;
        }
    }

    private static final class RecordingApkInstaller extends ApkInstaller {
        final List<File> installedFiles = new ArrayList<>();

        RecordingApkInstaller(Activity activity) {
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

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
            lastTapAction = onTapped;
        }

        @Override
        public void hide() {
        }
    }

    @Before
    public void resetDownloadGuard() {
        ApkUpdateUiController.DOWNLOAD_IN_PROGRESS.set(false);
        ShadowToast.reset();
    }

    private Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).create().start().resume().get();
    }

    @Test
    public void theIndicatorIsNotOfferedWhileTheApkIsStillBeingPrepared() {
        Activity activity = newActivity();
        DownloadNeverCompletingUpdateManager manager = new DownloadNeverCompletingUpdateManager(activity);
        ApkUpdateUiController controller =
            new ApkUpdateUiController(activity, manager, new RecordingApkInstaller(activity));

        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());
        RecordingIndicatorView indicatorViewWhilePreparing = new RecordingIndicatorView();
        controller.checkAndShowFloatingIndicator(indicatorViewWhilePreparing);

        Assert.assertEquals("the first check must be the one that starts the preparation", 1, manager.downloadCount);
        Assert.assertTrue("offering the button while the APK it needs is still downloading presents an update"
                + " that cannot be installed, and tapping it reaches the guard that discards the tap",
            indicatorViewWhilePreparing.shownVersions.isEmpty());
    }

    @Test
    public void aTapThatCannotStartTheInstallTellsTheUserInsteadOfDoingNothing() {
        Activity activity = newActivity();
        DownloadNeverCompletingUpdateManager manager = new DownloadNeverCompletingUpdateManager(activity);
        RecordingApkInstaller installer = new RecordingApkInstaller(activity);
        ApkUpdateUiController controller = new ApkUpdateUiController(activity, manager, installer);

        controller.checkAndShowFloatingIndicator(new RecordingIndicatorView());
        RecordingIndicatorView reopenedIndicatorView = new RecordingIndicatorView();
        controller.showPendingIndicatorIfAny(reopenedIndicatorView);
        Assert.assertNotNull("the stored update must still surface a button to tap",
            reopenedIndicatorView.lastTapAction);

        ShadowToast.reset();
        reopenedIndicatorView.lastTapAction.run();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertTrue("nothing can be installed while the APK has not finished downloading",
            installer.installedFiles.isEmpty());
        Assert.assertEquals("a tap that cannot start the install must say so, because a tap that returns"
                + " without a toast, a dialog or any other change looks to the user like a dead button",
            activity.getString(R.string.apk_update_preparing), ShadowToast.getTextOfLatestToast());
    }
}
