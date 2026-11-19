package com.termux.app.apkupdate;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class DownloadSlotWaiter {

    private static final long POLL_INTERVAL_MILLIS = 500L;

    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final BooleanSupplier busy;
    private final Sleeper sleeper;
    private final LongSupplier clockMillis;
    private final long timeoutMillis;

    public DownloadSlotWaiter(BooleanSupplier busy, Sleeper sleeper, LongSupplier clockMillis, long timeoutMillis) {
        this.busy = busy;
        this.sleeper = sleeper;
        this.clockMillis = clockMillis;
        this.timeoutMillis = timeoutMillis;
    }

    public boolean awaitIdle() throws InterruptedException {
        long startMillis = clockMillis.getAsLong();
        if (!busy.getAsBoolean()) {
            return true;
        }
        while (true) {
            if (clockMillis.getAsLong() - startMillis >= timeoutMillis) {
                return false;
            }
            sleeper.sleep(POLL_INTERVAL_MILLIS);
            if (!busy.getAsBoolean()) {
                return true;
            }
        }
    }
}
