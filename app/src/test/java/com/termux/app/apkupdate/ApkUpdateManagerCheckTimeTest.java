package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateManagerCheckTimeTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
    }

    @Test
    public void lastCheckTimeDefaultsToNoCheckTimeWhenNeverRecorded() {
        Assert.assertEquals(ApkUpdateManager.NO_CHECK_TIME, ApkUpdateManager.getLastCheckTime(context));
    }

    @Test
    public void recordCheckTimePersistsTheTimestampForLaterReads() {
        ApkUpdateManager.recordCheckTime(context, 1_000_000_000_000L);

        Assert.assertEquals(1_000_000_000_000L, ApkUpdateManager.getLastCheckTime(context));
    }

    @Test
    public void autoCheckIsDisabledByDefault() {
        Assert.assertFalse(ApkUpdateManager.isAutoCheckEnabled(context));
    }

    @Test
    public void autoCheckDecisionFollowsTheEnabledToggle() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(ApkUpdateManager.PREFERENCE_KEY_AUTO_CHECK, true).commit();

        Assert.assertTrue(ApkUpdateManager.isAutoCheckEnabled(context));
    }
}
