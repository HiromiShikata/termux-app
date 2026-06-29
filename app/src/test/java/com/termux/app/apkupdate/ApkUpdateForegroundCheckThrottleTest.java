package com.termux.app.apkupdate;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceManager;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateForegroundCheckThrottleTest {

    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;

    private SharedPreferences newPreferences() {
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().commit();
        return preferences;
    }

    @Test
    public void allowsCheckOnFirstRunWhenNeverChecked() {
        ApkUpdateForegroundCheckThrottle throttle =
            new ApkUpdateForegroundCheckThrottle(newPreferences(), ONE_MINUTE_MILLIS);

        Assert.assertTrue(throttle.shouldCheckNow(1000L));
    }

    @Test
    public void suppressesCheckWithinTheShortInterval() {
        ApkUpdateForegroundCheckThrottle throttle =
            new ApkUpdateForegroundCheckThrottle(newPreferences(), ONE_MINUTE_MILLIS);

        throttle.recordCheckedAt(10L * ONE_MINUTE_MILLIS);

        Assert.assertFalse(throttle.shouldCheckNow(10L * ONE_MINUTE_MILLIS + ONE_MINUTE_MILLIS / 2L));
    }

    @Test
    public void allowsCheckAfterTheShortIntervalElapsed() {
        ApkUpdateForegroundCheckThrottle throttle =
            new ApkUpdateForegroundCheckThrottle(newPreferences(), ONE_MINUTE_MILLIS);

        throttle.recordCheckedAt(10L * ONE_MINUTE_MILLIS);

        Assert.assertTrue(throttle.shouldCheckNow(10L * ONE_MINUTE_MILLIS + ONE_MINUTE_MILLIS));
    }

    @Test
    public void allowsCheckExactlyAtTheIntervalBoundary() {
        ApkUpdateForegroundCheckThrottle throttle =
            new ApkUpdateForegroundCheckThrottle(newPreferences(), ONE_MINUTE_MILLIS);

        throttle.recordCheckedAt(0L);

        Assert.assertTrue(throttle.shouldCheckNow(ONE_MINUTE_MILLIS));
    }

    @Test
    public void defaultIntervalIsOneMinute() {
        Assert.assertEquals(ONE_MINUTE_MILLIS, ApkUpdateForegroundCheckThrottle.MINIMUM_INTERVAL_MILLIS);
    }
}
