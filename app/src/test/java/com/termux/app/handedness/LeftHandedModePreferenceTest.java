package com.termux.app.handedness;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LeftHandedModePreferenceTest {

    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        preferences.setLeftHandedModeEnabled(false);
    }

    @Test
    public void leftHandedModeConstantsDefineThePreferenceKeyAndDefault() {
        Assert.assertEquals("left_handed_mode",
            TermuxPreferenceConstants.TERMUX_APP.KEY_LEFT_HANDED_MODE);
        Assert.assertFalse(TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_LEFT_HANDED_MODE);
    }

    @Test
    public void leftHandedModePreferenceIsDisabledByDefault() {
        Assert.assertFalse(preferences.isLeftHandedModeEnabled());
    }

    @Test
    public void leftHandedModePreferenceCanBeEnabledAndDisabled() {
        preferences.setLeftHandedModeEnabled(true);
        Assert.assertTrue(preferences.isLeftHandedModeEnabled());

        preferences.setLeftHandedModeEnabled(false);
        Assert.assertFalse(preferences.isLeftHandedModeEnabled());
    }
}
