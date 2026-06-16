package com.termux.app.terminal;

import android.app.Activity;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class DisabledSessionNamesPersistenceTest {

    @Test
    public void disabledSessionNamesKeyAndDefaultAreStable() {
        Assert.assertEquals("disabled_session_names",
            TermuxPreferenceConstants.TERMUX_APP.KEY_DISABLED_SESSION_NAMES);
        Assert.assertEquals("",
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_DISABLED_SESSION_NAMES);
    }

    @Test
    public void parseSplitsNewlineSeparatedNamesAndTrimsBlankLines() {
        Set<String> names = TermuxAppSharedPreferences.parseDisabledSessionNames("  alpha \n\n beta \n");

        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("alpha", "beta")), names);
    }

    @Test
    public void parseReturnsEmptySetForNullValue() {
        Assert.assertTrue(TermuxAppSharedPreferences.parseDisabledSessionNames(null).isEmpty());
    }

    @Test
    public void serializeAndParseRoundTripPreservesNames() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList("alpha", "gamma"));

        String serialized = TermuxAppSharedPreferences.serializeDisabledSessionNames(names);

        Assert.assertEquals(names, TermuxAppSharedPreferences.parseDisabledSessionNames(serialized));
    }

    @Test
    public void disabledStatePersistsThroughANewlyBuiltPreferencesInstance() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        prefs.setDisabledSessionNames("alpha");

        TermuxAppSharedPreferences reloaded = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(reloaded);
        Assert.assertTrue(reloaded.isSessionDisabled("alpha"));
        Assert.assertFalse(reloaded.isSessionDisabled("beta"));
    }

    @Test
    public void toggleAddsThenRemovesASessionNameAndReportsTheNewState() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        boolean disabledAfterFirstToggle = prefs.toggleSessionDisabled("alpha");
        Assert.assertTrue(disabledAfterFirstToggle);
        Assert.assertEquals(new HashSet<>(java.util.Collections.singletonList("alpha")),
            prefs.getDisabledSessionNames());

        boolean disabledAfterSecondToggle = prefs.toggleSessionDisabled("alpha");
        Assert.assertFalse(disabledAfterSecondToggle);
        Assert.assertTrue(prefs.getDisabledSessionNames().isEmpty());
    }

    @Test
    public void toggleIsANoOpForNullOrEmptyName() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);

        Assert.assertFalse(prefs.toggleSessionDisabled(null));
        Assert.assertFalse(prefs.toggleSessionDisabled(""));
        Assert.assertTrue(prefs.getDisabledSessionNames().isEmpty());
    }
}
