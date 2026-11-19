package com.termux.app.apkupdate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class BackgroundDownloadRun {

    public enum Status {
        RAN,
        TIMED_OUT,
        SLOT_LOST
    }

    public static final class Result<T> {

        private final Status status;
        private final Optional<T> value;

        private Result(Status status, Optional<T> value) {
            this.status = status;
            this.value = value;
        }

        public Status getStatus() {
            return status;
        }

        public Optional<T> getValue() {
            return value;
        }
    }

    private final AtomicBoolean downloadInProgress;
    private final DownloadSlotWaiter slotWaiter;

    public BackgroundDownloadRun(AtomicBoolean downloadInProgress, DownloadSlotWaiter slotWaiter) {
        this.downloadInProgress = downloadInProgress;
        this.slotWaiter = slotWaiter;
    }

   public <T> Result<T> run(Supplier<T> action) throws InterruptedException {
        if (!slotWaiter.awaitIdle()) {
            return new Result<>(Status.TIMED_OUT, Optional.empty());
        }
        if (!downloadInProgress.compareAndSet(false, true)) {
            return new Result<>(Status.SLOT_LOST, Optional.empty());
        }
        try {
            return new Result<>(Status.RAN, Optional.of(action.get()));
        } finally {
            downloadInProgress.set(false);
        }
    }

    public <T> Optional<T> runExclusively(Supplier<T> action) throws InterruptedException {
        return run(action).getValue();
    }
}
