package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DiagnosticsPhantomProcessMonitor {

    public static final String UNMEASURED_MONITOR_FLAG_VALUE = "<unmeasured>";

    public static final DiagnosticsPhantomProcessMonitor UNMEASURED =
        new DiagnosticsPhantomProcessMonitor(UNMEASURED_MONITOR_FLAG_VALUE, null, false);

    @NonNull
    private final String mMonitorFlagValue;

    @Nullable
    private final Integer mEnforcedMaximumPhantomProcesses;

    private final boolean mMonitorCanBeSwitchedOff;

    public DiagnosticsPhantomProcessMonitor(@NonNull String monitorFlagValue,
                                            @Nullable Integer enforcedMaximumPhantomProcesses,
                                            boolean monitorCanBeSwitchedOff) {
        mMonitorFlagValue = monitorFlagValue;
        mEnforcedMaximumPhantomProcesses = enforcedMaximumPhantomProcesses;
        mMonitorCanBeSwitchedOff = monitorCanBeSwitchedOff;
    }

    @NonNull
    public String getMonitorFlagValue() {
        return mMonitorFlagValue;
    }

    @Nullable
    public Integer getEnforcedMaximumPhantomProcesses() {
        return mEnforcedMaximumPhantomProcesses;
    }

    public boolean getMonitorCanBeSwitchedOff() {
        return mMonitorCanBeSwitchedOff;
    }
}
