package com.termux.app.apkupdate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class BackgroundDownloadRun {

    private final AtomicBoolean downloadInProgress;
    private final DownloadSlotWaiter slotWaiter;

    public BackgroundDownloadRun(AtomicBoolean downloadInProgress, DownloadSlotWaiter slotWaiter) {
        this.downloadInProgress = downloadInProgress;
        this.slotWaiter = slotWaiter;
    }

    public <T> Optional<T> runExclusively(Supplier<T> action) throws InterruptedException {
        if (!slotWaiter.awaitIdle()) {
            return Optional.empty();
        }
        if (!downloadInProgress.compareAndSet(false, true)) {
            return Optional.empty();
        }
        try {
            return Optional.of(action.get());
        } finally {
            downloadInProgress.set(false);
        }
    }
}
