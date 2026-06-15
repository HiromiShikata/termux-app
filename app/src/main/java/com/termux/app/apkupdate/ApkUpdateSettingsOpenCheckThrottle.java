package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public final class ApkUpdateSettingsOpenCheckThrottle {

    public static final String PREFERENCE_KEY_LAST_SETTINGS_OPEN_CHECK_AT =
        "apk_update_last_settings_open_check_at_millis";

    public static final long MINIMUM_INTERVAL_MILLIS = 5L * 1000L;

    private static final long NEVER_CHECKED = -1L;

    private final SharedPreferences preferences;
    private final long minimumIntervalMillis;

    public ApkUpdateSettingsOpenCheckThrottle(Context context) {
        this(PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()),
            MINIMUM_INTERVAL_MILLIS);
    }

    public ApkUpdateSettingsOpenCheckThrottle(SharedPreferences preferences, long minimumIntervalMillis) {
        this.preferences = preferences;
        this.minimumIntervalMillis = minimumIntervalMillis;
    }

    public boolean shouldCheckNow(long nowMillis) {
        long lastCheckedAtMillis = preferences.getLong(PREFERENCE_KEY_LAST_SETTINGS_OPEN_CHECK_AT, NEVER_CHECKED);
        if (lastCheckedAtMillis == NEVER_CHECKED) {
            return true;
        }
        return nowMillis - lastCheckedAtMillis >= minimumIntervalMillis;
    }

    public void recordCheckedAt(long nowMillis) {
        preferences.edit().putLong(PREFERENCE_KEY_LAST_SETTINGS_OPEN_CHECK_AT, nowMillis).apply();
    }
}
