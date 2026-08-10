package com.termux.app.phantomprocess;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.app.diagnostics.DiagnosticsPhantomProcessMonitor;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.android.PhantomProcessUtils;

public final class PhantomProcessMonitorStateReader {

    @NonNull
    public DiagnosticsPhantomProcessMonitor readOffTheMainThread(@NonNull Context context) {
        String monitorFlagValue =
            PhantomProcessUtils.getFeatureFlagMonitorPhantomProcsValueString(context).getName();
        Integer enforcedMaximum = PhantomProcessUtils.getActivityManagerMaxPhantomProcesses(context);
        boolean monitorCanBeSwitchedOff = PermissionUtils.checkPermissions(context,
            new String[]{Manifest.permission.WRITE_SECURE_SETTINGS});
        return new DiagnosticsPhantomProcessMonitor(monitorFlagValue, enforcedMaximum, monitorCanBeSwitchedOff);
    }
}
