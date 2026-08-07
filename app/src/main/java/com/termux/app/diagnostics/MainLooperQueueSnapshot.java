package com.termux.app.diagnostics;

import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class MainLooperQueueSnapshot {

    private MainLooperQueueSnapshot() {
    }

    @NonNull
    public static DiagnosticsMainLooperQueue take() {
        return DiagnosticsMainLooperQueue.parse(dumpMainLooper());
    }

    @NonNull
    static List<String> dumpMainLooper() {
        List<String> dumpLines = new ArrayList<>();
        Looper.getMainLooper().dump(dumpLines::add, "");
        return dumpLines;
    }
}
