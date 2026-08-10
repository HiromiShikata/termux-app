package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

public final class PhantomProcessMonitorStateHolder {

    private static final PhantomProcessMonitorStateHolder INSTANCE = new PhantomProcessMonitorStateHolder();

    @NonNull
    private final AtomicReference<DiagnosticsPhantomProcessMonitor> mState =
        new AtomicReference<>(DiagnosticsPhantomProcessMonitor.UNMEASURED);

    @NonNull
    public static PhantomProcessMonitorStateHolder getInstance() {
        return INSTANCE;
    }

    @NonNull
    public DiagnosticsPhantomProcessMonitor snapshot() {
        return mState.get();
    }

    public void record(@NonNull DiagnosticsPhantomProcessMonitor state) {
        mState.set(state);
    }
}
