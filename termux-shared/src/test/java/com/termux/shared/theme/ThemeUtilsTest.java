package com.termux.shared.theme;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ThemeUtilsTest {

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void isNightModeEnabledReturnsFalseForNullContext() {
        Assert.assertFalse(ThemeUtils.isNightModeEnabled(null));
    }

    @Test
    @Config(qualifiers = "night")
    public void isNightModeEnabledReturnsTrueWhenSystemInNightMode() {
        Assert.assertTrue(ThemeUtils.isNightModeEnabled(context()));
    }

    @Test
    @Config(qualifiers = "notnight")
    public void isNightModeEnabledReturnsFalseWhenSystemNotInNightMode() {
        Assert.assertFalse(ThemeUtils.isNightModeEnabled(context()));
    }

    @Test
    public void shouldEnableDarkThemeReturnsTrueForTrueMode() {
        Assert.assertTrue(ThemeUtils.shouldEnableDarkTheme(context(), NightMode.TRUE.getName()));
    }

    @Test
    public void shouldEnableDarkThemeReturnsFalseForFalseMode() {
        Assert.assertFalse(ThemeUtils.shouldEnableDarkTheme(context(), NightMode.FALSE.getName()));
    }

    @Test
    @Config(qualifiers = "night")
    public void shouldEnableDarkThemeFollowsSystemForSystemModeWhenNight() {
        Assert.assertTrue(ThemeUtils.shouldEnableDarkTheme(context(), NightMode.SYSTEM.getName()));
    }

    @Test
    @Config(qualifiers = "notnight")
    public void shouldEnableDarkThemeFollowsSystemForSystemModeWhenNotNight() {
        Assert.assertFalse(ThemeUtils.shouldEnableDarkTheme(context(), NightMode.SYSTEM.getName()));
    }

    @Test
    public void shouldEnableDarkThemeReturnsFalseForUnknownMode() {
        Assert.assertFalse(ThemeUtils.shouldEnableDarkTheme(context(), "unrecognized-mode"));
    }

    @Test
    public void shouldEnableDarkThemeReturnsFalseForNullMode() {
        Assert.assertFalse(ThemeUtils.shouldEnableDarkTheme(context(), null));
    }
}
