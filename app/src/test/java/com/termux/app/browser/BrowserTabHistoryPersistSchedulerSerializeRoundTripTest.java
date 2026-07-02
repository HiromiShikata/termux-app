package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserTabHistoryPersistSchedulerSerializeRoundTripTest {

    private static final class ManualDebouncer implements BrowserTabHistoryPersistScheduler.Debouncer {
        private Runnable mPending;

        @Override
        public void schedule(Runnable task) {
            mPending = task;
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
    }

    @Test
    public void offThreadSerializeRoundTripPreservesEntries() throws Exception {
        ManualDebouncer debouncer = new ManualDebouncer();
        BrowserTabHistorySerializer serializer = new BrowserTabHistorySerializer();
        List<String> backgroundEvents = new ArrayList<>();
        List<String> writes = new ArrayList<>();
        BrowserTabHistoryPersistScheduler scheduler = new BrowserTabHistoryPersistScheduler(
            debouncer,
            task -> {
                backgroundEvents.add("background");
                task.run();
            },
            history -> {
                try {
                    return serializer.serialize(history);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },
            writes::add);

        BrowserTabHistory source = new BrowserTabHistory()
            .recorded("https://example.com/a", "Alpha", "alpha body")
            .recorded("https://example.com/b", "Beta", "beta body");
        scheduler.markDirty(source);
        debouncer.fire();

        Assert.assertEquals(List.of("background"), backgroundEvents);
        Assert.assertEquals(1, writes.size());
        BrowserTabHistory restored =
            serializer.deserialize(writes.get(0), BrowserTabHistory.DEFAULT_MAX_ENTRIES);
        Assert.assertTrue(restored.hasSameEntriesAs(source));
    }
}
