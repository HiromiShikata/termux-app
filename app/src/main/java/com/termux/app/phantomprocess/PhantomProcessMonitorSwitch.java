package com.termux.app.phantomprocess;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PhantomProcessUtils;

public final class PhantomProcessMonitorSwitch {

    public static final String MONITOR_OFF_VALUE = "false";

    public interface GlobalSettingStore {

        void putString(@NonNull String key, @NonNull String value);

        @Nullable
        String getString(@NonNull String key);
    }

    @NonNull
    private final GlobalSettingStore mGlobalSettingStore;

    public PhantomProcessMonitorSwitch(@NonNull GlobalSettingStore globalSettingStore) {
        mGlobalSettingStore = globalSettingStore;
    }

    @NonNull
    public PhantomProcessMonitorSwitchResult switchTheMonitorOff() {
        try {
            mGlobalSettingStore.putString(
                PhantomProcessUtils.FEATURE_FLAG_SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS, MONITOR_OFF_VALUE);
        } catch (SecurityException e) {
            return PhantomProcessMonitorSwitchResult.refusedBySystem(String.valueOf(e.getMessage()));
        }
        return PhantomProcessMonitorSwitchResult.writtenAndReadBackAs(
            mGlobalSettingStore.getString(PhantomProcessUtils.FEATURE_FLAG_SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS));
    }
}
