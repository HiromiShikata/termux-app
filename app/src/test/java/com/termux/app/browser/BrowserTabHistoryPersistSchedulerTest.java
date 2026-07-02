package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BrowserTabHistoryPersistSchedulerTest {

    private static final class ManualDebouncer implements BrowserTabHistoryPersistScheduler.Debouncer {
        private Runnable mPending;
        private int mScheduleCount;

        @Override
        public void schedule(Runnable task) {
            mPending = task;
            mScheduleCount++;
        }

        @Override
        public void cancel() {
            mPending = null;
        }

        void fire() {
            Runnable task = mPending;
            mPending = null;
            if (task != null) task.run();
        }

        boolean hasPending() {
            return mPending != null;
        }
    }

    private static BrowserTabHistory historyWith(String url, String title) {
        return new BrowserTabHistory().recorded(url, title);
    }

    @Test
    public void burstOfRecordsProducesSingleSerializeAndWrite() {
        ManualDebouncer debouncer = new ManualDebouncer();
        AtomicInteger serializeCount = new AtomicInteger();
        List<String> writes = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            Runnable::run,
            history -> {
                serializeCount.incrementAndGet();
                return String.valueOf(history.getEntries().size());
            },
            writes::add);

        scheduler.markDirty(historyWith("https://example.com/a", "A"));
        scheduler.markDirty(historyWith("https://example.com/b", "B"));
        scheduler.markDirty(historyWith("https://example.com/c", "C"));

        Assert.assertEquals(0, serializeCount.get());
        Assert.assertTrue(writes.isEmpty());

        debouncer.fire();

        Assert.assertEquals(1, serializeCount.get());
        Assert.assertEquals(1, writes.size());
    }

    @Test
    public void debouncedWriteUsesLatestSnapshot() {
        ManualDebouncer debouncer = new ManualDebouncer();
        List<String> writes = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            Runnable::run,
            history -> history.getEntries().get(0).getTitle(),
            writes::add);

        scheduler.markDirty(historyWith("https://example.com/a", "First"));
        scheduler.markDirty(historyWith("https://example.com/b", "Latest"));
        debouncer.fire();

        Assert.assertEquals(1, writes.size());
        Assert.assertEquals("Latest", writes.get(0));
    }

    @Test
    public void serializeRunsOnBackgroundRunnerForDebouncedWrite() {
        ManualDebouncer debouncer = new ManualDebouncer();
        List<String> events = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            task -> {
                events.add("background-start");
                task.run();
                events.add("background-end");
            },
            history -> {
                events.add("serialize");
                return "value";
            },
            value -> events.add("write"));

        scheduler.markDirty(historyWith("https://example.com/a", "A"));
        debouncer.fire();

        Assert.assertEquals(
            List.of("background-start", "serialize", "write", "background-end"), events);
    }

    @Test
    public void flushNowWritesSynchronouslyWithoutBackgroundRunner() {
        ManualDebouncer debouncer = new ManualDebouncer();
        List<String> events = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            task -> {
                events.add("background");
                task.run();
            },
            history -> "serialized",
            value -> events.add("write:" + value));

        scheduler.markDirty(historyWith("https://example.com/a", "A"));
        scheduler.flushNow();

        Assert.assertEquals(List.of("write:serialized"), events);
        Assert.assertFalse(debouncer.hasPending());
        Assert.assertFalse(scheduler.hasPendingWrite());
    }

    @Test
    public void flushNowWithoutPendingWriteDoesNothing() {
        ManualDebouncer debouncer = new ManualDebouncer();
        List<String> writes = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            Runnable::run,
            history -> "value",
            writes::add);

        scheduler.flushNow();

        Assert.assertTrue(writes.isEmpty());
    }

    @Test
    public void firingDebouncerClearsPendingWriteFlag() {
        ManualDebouncer debouncer = new ManualDebouncer();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            Runnable::run,
            history -> "value",
            value -> {
            });

        scheduler.markDirty(historyWith("https://example.com/a", "A"));
        Assert.assertTrue(scheduler.hasPendingWrite());
        debouncer.fire();
        Assert.assertFalse(scheduler.hasPendingWrite());
    }
}
