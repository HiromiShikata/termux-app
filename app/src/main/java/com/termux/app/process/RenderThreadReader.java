package com.termux.app.process;

import androidx.annotation.NonNull;

import com.termux.app.diagnostics.DiagnosticsRenderThread;

import java.util.List;

public final class RenderThreadReader {

    private static final long MILLIS_PER_SECOND = 1000L;

    @NonNull
    private final ThreadTable mThreadTable;

    private final long mClockTicksPerSecond;

    public RenderThreadReader(@NonNull ThreadTable threadTable, long clockTicksPerSecond) {
        mThreadTable = threadTable;
        mClockTicksPerSecond = clockTicksPerSecond;
    }

    @NonNull
    public DiagnosticsRenderThread read() {
        if (mClockTicksPerSecond <= 0L) {
            return DiagnosticsRenderThread.readFailed(
                "the clock tick rate this platform reported cannot convert processor time, and was "
                    + mClockTicksPerSecond);
        }
        List<ProcessThread> threads;
        try {
            threads = mThreadTable.threads();
        } catch (IllegalStateException e) {
            return DiagnosticsRenderThread.readFailed(String.valueOf(e.getMessage()));
        }
        for (ProcessThread thread : threads) {
            if (DiagnosticsRenderThread.THREAD_NAME.equals(thread.getName())) {
                return DiagnosticsRenderThread.measured(thread.getSchedulerState(),
                    millisOf(thread.getUserTimeTicks()), millisOf(thread.getSystemTimeTicks()));
            }
        }
        return DiagnosticsRenderThread.threadAbsent();
    }

    private long millisOf(long ticks) {
        return ticks * MILLIS_PER_SECOND / mClockTicksPerSecond;
    }
}
