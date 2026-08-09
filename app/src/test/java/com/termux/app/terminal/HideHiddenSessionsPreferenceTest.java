package com.termux.app.terminal;

import android.app.Activity;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HideHiddenSessionsPreferenceTest {

    @Before
    public void resetHideHiddenSessionsToDefault() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        if (prefs != null) prefs.setHideHiddenSessions(false);
    }

    @Test
    public void hideHiddenSessionsKeyAndDefaultAreStable() {
        Assert.assertEquals("hide_hidden_sessions",
            TermuxPreferenceConstants.TERMUX_APP.KEY_HIDE_HIDDEN_SESSIONS);
        Assert.assertFalse(TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_HIDE_HIDDEN_SESSIONS);
    }

    @Test
    public void hideHiddenSessionsDefaultsToFalse() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        Assert.assertFalse(prefs.shouldHideHiddenSessions());
    }

    @Test
    public void toggleFlipsStateAndReportsTheNewValue() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        boolean afterFirstToggle = prefs.toggleHideHiddenSessions();
        Assert.assertTrue(afterFirstToggle);
        Assert.assertTrue(prefs.shouldHideHiddenSessions());

        boolean afterSecondToggle = prefs.toggleHideHiddenSessions();
        Assert.assertFalse(afterSecondToggle);
        Assert.assertFalse(prefs.shouldHideHiddenSessions());
    }

    @Test
    public void hideHiddenSessionsStatePersistsThroughANewlyBuiltPreferencesInstance() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        prefs.setHideHiddenSessions(true);

        TermuxAppSharedPreferences reloaded = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(reloaded);
        Assert.assertTrue(reloaded.shouldHideHiddenSessions());
    }
}
