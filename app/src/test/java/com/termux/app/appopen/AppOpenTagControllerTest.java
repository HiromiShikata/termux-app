package com.termux.app.appopen;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppOpenTagControllerTest {

    private static final String SESSION_A_HANDLE = "handle-session-a";
    private static final String SESSION_B_HANDLE = "handle-session-b";
    private static final String PACKAGE_A = "com.example.a";
    private static final String PACKAGE_B = "com.example.b";

    private static final class RecordingAppLauncher implements AppOpenTagController.AppLauncher {
        final List<String> launchedPackageIds = new ArrayList<>();

        @Override
        public void launchApp(@NonNull String packageId) {
            launchedPackageIds.add(packageId);
        }
    }

    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        Assert.assertNotNull(preferences);
        preferences.setOpenTagAutoOpenEnabled(true);
    }

    @Test
    public void launchesDetectedPackageIdFromTheSessionThatProducedTheAppOpenTag() {
        RecordingAppLauncher launcher = new RecordingAppLauncher();
        AppOpenTagController controller = new AppOpenTagController(preferences, launcher);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        Assert.assertEquals(List.of(PACKAGE_A), launcher.launchedPackageIds);
    }

    @Test
    public void doesNotRelaunchTheSamePackageIdForTheSameSession() {
        RecordingAppLauncher launcher = new RecordingAppLauncher();
        AppOpenTagController controller = new AppOpenTagController(preferences, launcher);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");
        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        Assert.assertEquals(1, launcher.launchedPackageIds.size());
    }

    @Test
    public void reScanningTheSameOutputDoesNotRelaunchButANewPackageIdStillLaunches() {
        RecordingAppLauncher launcher = new RecordingAppLauncher();
        AppOpenTagController controller = new AppOpenTagController(preferences, launcher);

        String transcriptWithFirstPackage = "<app-open>" + PACKAGE_A + "</app-open>";
        controller.onSessionTextChanged(SESSION_A_HANDLE, transcriptWithFirstPackage);
        Assert.assertEquals(1, launcher.launchedPackageIds.size());

        for (int reScan = 0; reScan < 5; reScan++) {
            controller.onSessionTextChanged(SESSION_A_HANDLE, transcriptWithFirstPackage);
        }
        Assert.assertEquals(1, launcher.launchedPackageIds.size());

        controller.onSessionTextChanged(SESSION_A_HANDLE,
            "<app-open>" + PACKAGE_A + "</app-open>\noutput\n<app-open>" + PACKAGE_B + "</app-open>");

        Assert.assertEquals(List.of(PACKAGE_A, PACKAGE_B), launcher.launchedPackageIds);
    }

    @Test
    public void tracksPerSessionScannerStateSoEachSessionLaunchesItsOwnPackageId() {
        RecordingAppLauncher launcher = new RecordingAppLauncher();
        AppOpenTagController controller = new AppOpenTagController(preferences, launcher);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");
        controller.onSessionTextChanged(SESSION_B_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        Assert.assertEquals(List.of(PACKAGE_A, PACKAGE_A), launcher.launchedPackageIds);
    }

    @Test
    public void doesNotRelaunchAlreadyLaunchedPackageIdAcrossActivityRecreationsThatReRegisterTheLauncher() {
        AppOpenTagController controller = new AppOpenTagController(preferences, null);
        String stillVisibleTranscript = "<app-open>" + PACKAGE_A + "</app-open>";

        RecordingAppLauncher firstActivityLauncher = new RecordingAppLauncher();
        controller.setAppLauncher(firstActivityLauncher);
        controller.onSessionTextChanged(SESSION_A_HANDLE, stillVisibleTranscript);

        for (int activityRecreation = 0; activityRecreation < 5; activityRecreation++) {
            controller.setAppLauncher(null);
            RecordingAppLauncher recreatedActivityLauncher = new RecordingAppLauncher();
            controller.setAppLauncher(recreatedActivityLauncher);
            controller.onSessionTextChanged(SESSION_A_HANDLE, stillVisibleTranscript);
            Assert.assertTrue(recreatedActivityLauncher.launchedPackageIds.isEmpty());
        }

        Assert.assertEquals(List.of(PACKAGE_A), firstActivityLauncher.launchedPackageIds);
    }

    @Test
    public void doesNotScanWhileNoForegroundLauncherIsRegistered() {
        AppOpenTagController controller = new AppOpenTagController(preferences, null);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        RecordingAppLauncher launcher = new RecordingAppLauncher();
        controller.setAppLauncher(launcher);
        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        Assert.assertEquals(List.of(PACKAGE_A), launcher.launchedPackageIds);
    }

    @Test
    public void doesNotLaunchWhenAutoOpenDisabled() {
        preferences.setOpenTagAutoOpenEnabled(false);
        RecordingAppLauncher launcher = new RecordingAppLauncher();
        AppOpenTagController controller = new AppOpenTagController(preferences, launcher);

        controller.onSessionTextChanged(SESSION_A_HANDLE, "<app-open>" + PACKAGE_A + "</app-open>");

        Assert.assertTrue(launcher.launchedPackageIds.isEmpty());
    }
}
