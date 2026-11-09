package com.termux.app.terminal.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TtsUtteranceCompletionsTest {

    private static final class Outcome implements TtsUtteranceOutcome {
        int spoken;
        String failure;

        @Override
        public void onSpoken() {
            spoken++;
        }

        @Override
        public void onFailed(String reason) {
            failure = reason;
        }
    }

    @Test
    public void eachRegisteredIdFinishesOnce() {
        TtsUtteranceCompletions completions = new TtsUtteranceCompletions();
        Outcome outcome = new Outcome();
        String id = completions.register(outcome);

        assertTrue(completions.finishSpoken(id));
        assertFalse(completions.finishSpoken(id));
        assertEquals(1, outcome.spoken);
    }

    @Test
    public void failedCompletionFinishesRegisteredIdOnly() {
        TtsUtteranceCompletions completions = new TtsUtteranceCompletions();
        Outcome outcome = new Outcome();
        String id = completions.register(outcome);

        assertFalse(completions.finishFailed("missing", "x"));
        assertTrue(completions.finishFailed(id, "reason"));
        assertEquals("reason", outcome.failure);
        assertFalse(completions.finishFailed(id, "again"));
    }

    @Test
    public void idsAreUnique() {
        TtsUtteranceCompletions completions = new TtsUtteranceCompletions();
        Outcome outcome = new Outcome();

        assertFalse(completions.register(outcome).equals(completions.register(outcome)));
    }

    @Test
    public void takeAllReturnsAllPendingOutcomesOnlyOnce() {
        TtsUtteranceCompletions completions = new TtsUtteranceCompletions();
        Outcome first = new Outcome();
        Outcome second = new Outcome();
        Outcome third = new Outcome();
        completions.register(first);
        completions.register(second);
        completions.register(third);

        List<TtsUtteranceOutcome> taken = completions.takeAll();

        assertEquals(3, taken.size());
        assertTrue(taken.contains(first));
        assertTrue(taken.contains(second));
        assertTrue(taken.contains(third));
        assertTrue(completions.takeAll().isEmpty());
    }

    @Test
    public void concurrentFinishAndTakeAllReportOutcomeExactlyOnce() throws Exception {
        TtsUtteranceCompletions completions = new TtsUtteranceCompletions();
        Outcome first = new Outcome();
        Outcome second = new Outcome();
        completions.register(first);
        String secondId = completions.register(second);
        CountDownLatch takeAllStarted = new CountDownLatch(1);
        CountDownLatch callbackFinished = new CountDownLatch(1);

        Thread callback = new Thread(() -> {
            try {
                assertTrue(takeAllStarted.await(5, TimeUnit.SECONDS));
                completions.finishSpoken(secondId);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                callbackFinished.countDown();
            }
        });
        callback.start();
        takeAllStarted.countDown();
        List<TtsUtteranceOutcome> taken = new ArrayList<>(completions.takeAll());
        assertTrue(callbackFinished.await(5, TimeUnit.SECONDS));
        callback.join(5000);

        int secondReports = second.spoken + (taken.contains(second) ? 1 : 0);
        assertEquals(1, secondReports);
        assertEquals(1, firstReports(taken, first));
        assertTrue(completions.takeAll().isEmpty());
    }

    private int firstReports(List<TtsUtteranceOutcome> taken, Outcome first) {
        return taken.contains(first) ? 1 : 0;
    }
}
