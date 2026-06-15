package com.termux.app.apkupdate;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceManager;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateSettingsOpenCheckThrottleTest {

    private static final long FIVE_SECONDS_MILLIS = 5L * 1000L;

    private SharedPreferences newPreferences() {
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().commit();
        return preferences;
    }

    @Test
    public void allowsCheckOnFirstSettingsOpenWhenNeverChecked() {
        ApkUpdateSettingsOpenCheckThrottle throttle =
            new ApkUpdateSettingsOpenCheckThrottle(newPreferences(), FIVE_SECONDS_MILLIS);

        Assert.assertTrue(throttle.shouldCheckNow(1000L));
    }

    @Test
    public void settingsOpenCheckIsAllowedRegardlessOfAutoCheckToggle() {
        SharedPreferences preferences = newPreferences();
        preferences.edit().putBoolean(ApkUpdateManager.PREFERENCE_KEY_AUTO_CHECK, false).commit();
        ApkUpdateSettingsOpenCheckThrottle throttle =
            new ApkUpdateSettingsOpenCheckThrottle(preferences, FIVE_SECONDS_MILLIS);

        Assert.assertFalse(ApkUpdateManager.isAutoCheckEnabled(RuntimeEnvironment.getApplication()));
        Assert.assertTrue(throttle.shouldCheckNow(1000L));
    }

    @Test
    public void suppressesSecondCheckFromRapidReentryWithinShortInterval() {
        ApkUpdateSettingsOpenCheckThrottle throttle =
            new ApkUpdateSettingsOpenCheckThrottle(newPreferences(), FIVE_SECONDS_MILLIS);

        throttle.recordCheckedAt(10_000L);

        Assert.assertFalse(throttle.shouldCheckNow(12_000L));
    }

    @Test
    public void allowsCheckAgainAfterShortIntervalElapsed() {
        ApkUpdateSettingsOpenCheckThrottle throttle =
            new ApkUpdateSettingsOpenCheckThrottle(newPreferences(), FIVE_SECONDS_MILLIS);

        throttle.recordCheckedAt(10_000L);

        Assert.assertTrue(throttle.shouldCheckNow(10_000L + FIVE_SECONDS_MILLIS));
    }

    @Test
    public void allowsCheckExactlyAtIntervalBoundary() {
        ApkUpdateSettingsOpenCheckThrottle throttle =
            new ApkUpdateSettingsOpenCheckThrottle(newPreferences(), FIVE_SECONDS_MILLIS);

        throttle.recordCheckedAt(0L);

        Assert.assertTrue(throttle.shouldCheckNow(FIVE_SECONDS_MILLIS));
    }

    @Test
    public void defaultIntervalIsAShortFewSecondsNotMultiHour() {
        Assert.assertEquals(5L * 1000L, ApkUpdateSettingsOpenCheckThrottle.MINIMUM_INTERVAL_MILLIS);
    }
}
