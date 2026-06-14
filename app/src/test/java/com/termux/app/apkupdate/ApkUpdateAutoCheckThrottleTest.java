package com.termux.app.apkupdate;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceManager;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateAutoCheckThrottleTest {

    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;

    private SharedPreferences newPreferences() {
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        preferences.edit().clear().commit();
        return preferences;
    }

    @Test
    public void allowsCheckOnFirstRunWhenNeverChecked() {
        ApkUpdateAutoCheckThrottle throttle =
            new ApkUpdateAutoCheckThrottle(newPreferences(), 6L * ONE_HOUR_MILLIS);

        Assert.assertTrue(throttle.shouldCheckNow(1000L));
    }

    @Test
    public void suppressesCheckWhenWithinMinimumInterval() {
        ApkUpdateAutoCheckThrottle throttle =
            new ApkUpdateAutoCheckThrottle(newPreferences(), 6L * ONE_HOUR_MILLIS);

        throttle.recordCheckedAt(10L * ONE_HOUR_MILLIS);

        Assert.assertFalse(throttle.shouldCheckNow(10L * ONE_HOUR_MILLIS + ONE_HOUR_MILLIS));
    }

    @Test
    public void allowsCheckAfterMinimumIntervalElapsed() {
        ApkUpdateAutoCheckThrottle throttle =
            new ApkUpdateAutoCheckThrottle(newPreferences(), 6L * ONE_HOUR_MILLIS);

        throttle.recordCheckedAt(10L * ONE_HOUR_MILLIS);

        Assert.assertTrue(throttle.shouldCheckNow(10L * ONE_HOUR_MILLIS + 6L * ONE_HOUR_MILLIS));
    }

    @Test
    public void allowsCheckExactlyAtIntervalBoundary() {
        ApkUpdateAutoCheckThrottle throttle =
            new ApkUpdateAutoCheckThrottle(newPreferences(), 6L * ONE_HOUR_MILLIS);

        throttle.recordCheckedAt(0L);

        Assert.assertTrue(throttle.shouldCheckNow(6L * ONE_HOUR_MILLIS));
    }
}
