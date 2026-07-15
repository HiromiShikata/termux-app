package com.termux.app.browser;

import android.app.Activity;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserMeetLowPowerVideoSettingsPersistenceTest {

    private TermuxAppSharedPreferences buildPreferences() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(preferences);
        return preferences;
    }

    @Test
    public void keysAndDefaultsAreStable() {
        Assert.assertEquals("browser_meet_low_power_video_enabled",
            TermuxPreferenceConstants.TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED);
        Assert.assertFalse(
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED);
        Assert.assertEquals(640,
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH);
        Assert.assertEquals(360,
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT);
        Assert.assertEquals(15,
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE);
    }

    @Test
    public void defaultsAreDisabledWithBaselineCaps() {
        TermuxAppSharedPreferences preferences = buildPreferences();

        Assert.assertFalse(preferences.isBrowserMeetLowPowerVideoEnabled());
        Assert.assertEquals(640, preferences.getBrowserMeetLowPowerVideoMaxWidth());
        Assert.assertEquals(360, preferences.getBrowserMeetLowPowerVideoMaxHeight());
        Assert.assertEquals(15, preferences.getBrowserMeetLowPowerVideoMaxFramerate());
    }

    @Test
    public void enabledFlagPersistsThroughReload() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setBrowserMeetLowPowerVideoEnabled(true);

        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences reloaded = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(reloaded);
        Assert.assertTrue(reloaded.isBrowserMeetLowPowerVideoEnabled());
    }

    @Test
    public void resolutionAndFrameratePersist() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setBrowserMeetLowPowerVideoMaxWidth(320);
        preferences.setBrowserMeetLowPowerVideoMaxHeight(180);
        preferences.setBrowserMeetLowPowerVideoMaxFramerate(5);

        Assert.assertEquals(320, preferences.getBrowserMeetLowPowerVideoMaxWidth());
        Assert.assertEquals(180, preferences.getBrowserMeetLowPowerVideoMaxHeight());
        Assert.assertEquals(5, preferences.getBrowserMeetLowPowerVideoMaxFramerate());
    }

    @Test
    public void fromPreferencesReadsPersistedValues() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setBrowserMeetLowPowerVideoEnabled(true);
        preferences.setBrowserMeetLowPowerVideoMaxWidth(480);
        preferences.setBrowserMeetLowPowerVideoMaxHeight(270);
        preferences.setBrowserMeetLowPowerVideoMaxFramerate(10);

        BrowserMeetLowPowerVideoSettings settings =
            BrowserMeetLowPowerVideoSettings.fromPreferences(preferences);

        Assert.assertTrue(settings.isEnabled());
        Assert.assertEquals(480, settings.getMaxWidth());
        Assert.assertEquals(270, settings.getMaxHeight());
        Assert.assertEquals(10, settings.getMaxFramerate());
    }
}
