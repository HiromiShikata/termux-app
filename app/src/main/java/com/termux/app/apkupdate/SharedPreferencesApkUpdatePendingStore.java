package com.termux.app.apkupdate;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public final class SharedPreferencesApkUpdatePendingStore implements ApkUpdatePendingState.Store {

    private final SharedPreferences preferences;

    public SharedPreferencesApkUpdatePendingStore(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    @Nullable
    @Override
    public String getString(String key) {
        return preferences.getString(key, null);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    @Override
    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }
}
