package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BackgroundDownloadRunTest {

    private static final long TIMEOUT_MILLIS = 10_000L;
    private static final long NEVER_RELEASED = -1L;
    private static final String RESULT = "downloaded";

    private static final class FakeSleeper implements DownloadSlotWaiter.Sleeper {

        private final AtomicLong nowMillis = new AtomicLong();
        private final AtomicBoolean downloadInProgress;
        private final long releaseAfterSleeps;
        private long sleepCount;

        FakeSleeper(AtomicBoolean downloadInProgress, long releaseAfterSleeps) {
            this.downloadInProgress = downloadInProgress;
            this.releaseAfterSleeps = releaseAfterSleeps;
        }

        @Override
        public void sleep(long millis) {
            sleepCount++;
            nowMillis.addAndGet(millis);
            if (sleepCount == releaseAfterSleeps) {
                downloadInProgress.set(false);
            }
        }

        long clockMillis() {
            return nowMillis.get();
        }
    }

    private static BackgroundDownloadRun newRun(AtomicBoolean downloadInProgress, FakeSleeper sleeper) {
        DownloadSlotWaiter slotWaiter =
            new DownloadSlotWaiter(downloadInProgress::get, sleeper, sleeper::clockMillis, TIMEOUT_MILLIS);
        return new BackgroundDownloadRun(downloadInProgress, slotWaiter);
    }

    @Test
    public void runsActionAndReleasesFlagWhenFlagIsFree() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(false);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, NEVER_RELEASED);
        AtomicInteger runs = new AtomicInteger();

        Optional<String> result = newRun(downloadInProgress, sleeper).runExclusively(() -> {
            runs.incrementAndGet();
            return RESULT;
        });

        Assert.assertEquals(Optional.of(RESULT), result);
        Assert.assertEquals(1, runs.get());
        Assert.assertFalse(downloadInProgress.get());
    }

    @Test
    public void skipsActionAfterTimeoutWhenFlagStaysHeld() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(true);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, NEVER_RELEASED);
        AtomicInteger runs = new AtomicInteger();

        Optional<String> result = newRun(downloadInProgress, sleeper).runExclusively(() -> {
            runs.incrementAndGet();
            return RESULT;
        });

        Assert.assertEquals(Optional.empty(), result);
        Assert.assertEquals(0, runs.get());
        Assert.assertTrue(downloadInProgress.get());
        Assert.assertEquals(TIMEOUT_MILLIS, sleeper.clockMillis());
    }

    @Test
    public void runsActionAfterFlagIsReleasedWhileWaiting() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(true);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, 3L);
        AtomicInteger runs = new AtomicInteger();

        Optional<String> result = newRun(downloadInProgress, sleeper).runExclusively(() -> {
            runs.incrementAndGet();
            return RESULT;
        });

        Assert.assertEquals(Optional.of(RESULT), result);
        Assert.assertEquals(1, runs.get());
        Assert.assertFalse(downloadInProgress.get());
    }

    @Test
    public void propagatesActionExceptionAndReleasesFlag() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(false);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, NEVER_RELEASED);
        AtomicInteger runs = new AtomicInteger();
        AtomicBoolean propagated = new AtomicBoolean(false);
        Supplier<String> failingAction = () -> {
            runs.incrementAndGet();
            throw new IllegalStateException("download failed");
        };

        try {
            newRun(downloadInProgress, sleeper).runExclusively(failingAction);
        } catch (IllegalStateException expected) {
            propagated.set(true);
        }

        Assert.assertTrue(propagated.get());
        Assert.assertEquals(1, runs.get());
        Assert.assertFalse(downloadInProgress.get());
    }

    @Test
    public void holdsFlagWhileActionRuns() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(false);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, NEVER_RELEASED);
        AtomicReference<Boolean> observedInsideAction = new AtomicReference<>();

        newRun(downloadInProgress, sleeper).runExclusively(() -> {
            observedInsideAction.set(downloadInProgress.get());
            return RESULT;
        });

        Assert.assertEquals(Boolean.TRUE, observedInsideAction.get());
    }

    @Test
    public void skipsActionWhenFlagIsClaimedBetweenIdleCheckAndCompareAndSet() throws InterruptedException {
        AtomicBoolean downloadInProgress = new AtomicBoolean(false);
        FakeSleeper sleeper = new FakeSleeper(downloadInProgress, NEVER_RELEASED);
        DownloadSlotWaiter claimingSlotWaiter = new DownloadSlotWaiter(
            () -> {
                downloadInProgress.set(true);
                return false;
            },
            sleeper,
            sleeper::clockMillis,
            TIMEOUT_MILLIS);
        AtomicInteger runs = new AtomicInteger();

        Optional<String> result = new BackgroundDownloadRun(downloadInProgress, claimingSlotWaiter).runExclusively(() -> {
            runs.incrementAndGet();
            return RESULT;
        });

        Assert.assertEquals(Optional.empty(), result);
        Assert.assertEquals(0, runs.get());
        Assert.assertTrue(downloadInProgress.get());
    }
}
