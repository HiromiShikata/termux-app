package com.termux.app.terminal;

import android.app.Activity;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.LinkedHashSet;

@RunWith(RobolectricTestRunner.class)
public class ShortcutNavigationSessionUnhiderTest {

    private TermuxAppSharedPreferences buildPreferences() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(preferences);
        return preferences;
    }

    @Test
    public void navigatingToAHiddenSessionRemovesItFromTheDisabledSetTheFilterReads() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setDisabledSessionNames("alpha\nbeta");

        boolean unhid = ShortcutNavigationSessionUnhider.unhideNavigatedSession(preferences, "alpha");

        Assert.assertTrue(unhid);
        Assert.assertFalse(preferences.isSessionDisabled("alpha"));
        Assert.assertTrue(preferences.isSessionDisabled("beta"));
    }

    @Test
    public void navigatingToAVisibleSessionLeavesTheDisabledSetUnchanged() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setDisabledSessionNames("beta");

        boolean unhid = ShortcutNavigationSessionUnhider.unhideNavigatedSession(preferences, "alpha");

        Assert.assertFalse(unhid);
        Assert.assertEquals(new LinkedHashSet<>(Collections.singletonList("beta")),
            preferences.getDisabledSessionNames());
    }

    @Test
    public void unhidePersistsAcrossAFreshlyBuiltPreferencesInstance() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(preferences);
        preferences.setDisabledSessionNames("alpha");

        ShortcutNavigationSessionUnhider.unhideNavigatedSession(preferences, "alpha");

        TermuxAppSharedPreferences reloaded = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(reloaded);
        Assert.assertFalse(reloaded.isSessionDisabled("alpha"));
    }

    @Test
    public void nullOrEmptySessionNameIsANoOp() {
        TermuxAppSharedPreferences preferences = buildPreferences();
        preferences.setDisabledSessionNames("alpha");

        Assert.assertFalse(ShortcutNavigationSessionUnhider.unhideNavigatedSession(preferences, null));
        Assert.assertFalse(ShortcutNavigationSessionUnhider.unhideNavigatedSession(preferences, ""));
        Assert.assertTrue(preferences.isSessionDisabled("alpha"));
    }
}
