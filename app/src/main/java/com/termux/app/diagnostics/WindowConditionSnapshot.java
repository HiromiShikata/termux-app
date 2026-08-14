package com.termux.app.diagnostics;

import android.view.View;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;

public final class WindowConditionSnapshot {

    private WindowConditionSnapshot() {
    }

    @NonNull
    public static DiagnosticsWindowCondition take(@NonNull TermuxActivity activity,
                                                  @NonNull DiagnosticsDrawTime windowDrawTime,
                                                  @NonNull DiagnosticsDrawTime terminalDrawTime) {
        View decorView = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decorView == null) return DiagnosticsWindowCondition.UNMEASURED;
        return DiagnosticsWindowCondition.measured(windowDrawTime, terminalDrawTime,
            describeWindowVisibility(decorView.getWindowVisibility()),
            decorView.isAttachedToWindow(), decorView.hasWindowFocus());
    }

    @NonNull
    private static String describeWindowVisibility(int windowVisibility) {
        if (windowVisibility == View.VISIBLE) return "VISIBLE";
        if (windowVisibility == View.INVISIBLE) return "INVISIBLE";
        if (windowVisibility == View.GONE) return "GONE";
        return String.valueOf(windowVisibility);
    }
}
