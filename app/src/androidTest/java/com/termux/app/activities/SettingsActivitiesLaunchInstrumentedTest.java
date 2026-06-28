package com.termux.app.activities;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsActivitiesLaunchInstrumentedTest {

    @Test
    public void settingsActivityReachesResumedWithRenderedRootPreferenceScreen() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.settings));
                Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.settings);
                assertNotNull(fragment);
                assertTrue(fragment instanceof SettingsActivity.RootPreferencesFragment);
                PreferenceScreen preferenceScreen = ((PreferenceFragmentCompat) fragment).getPreferenceScreen();
                assertNotNull(preferenceScreen);
                assertTrue(preferenceScreen.getPreferenceCount() > 0);
            });

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            assertTrue(
                "SettingsActivity must stay RESUMED after the deferred plugin-visibility work runs, "
                    + "so the preference RecyclerView layout pass is never mutated off the main thread",
                scenario.getState().isAtLeast(Lifecycle.State.RESUMED));
        }
    }

    @Test
    public void autosshConfigActivityReachesResumed() {
        assertReachesResumed(AutosshConfigActivity.class);
    }

    @Test
    public void sessionDefinitionConfigActivityReachesResumed() {
        assertReachesResumed(SessionDefinitionConfigActivity.class);
    }

    @Test
    public void alwaysNaSessionNamesConfigActivityReachesResumed() {
        assertReachesResumed(AlwaysNaSessionNamesConfigActivity.class);
    }

    @Test
    public void crashLogViewerActivityReachesResumed() {
        assertReachesResumed(CrashLogViewerActivity.class);
    }

    private static <T extends Activity> void assertReachesResumed(Class<T> activityClass) {
        try (ActivityScenario<T> scenario = ActivityScenario.launch(activityClass)) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }
}
