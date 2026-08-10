package com.termux.app.phantomprocess;

import android.content.ContentResolver;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SettingsGlobalStore implements PhantomProcessMonitorSwitch.GlobalSettingStore {

    @NonNull
    private final ContentResolver mContentResolver;

    public SettingsGlobalStore(@NonNull ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    @Override
    public void putString(@NonNull String key, @NonNull String value) {
        Settings.Global.putString(mContentResolver, key, value);
    }

    @Override
    @Nullable
    public String getString(@NonNull String key) {
        return Settings.Global.getString(mContentResolver, key);
    }
}
