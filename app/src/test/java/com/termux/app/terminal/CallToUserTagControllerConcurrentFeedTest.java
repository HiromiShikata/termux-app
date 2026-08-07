package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class CallToUserTagControllerConcurrentFeedTest {

    private static final int ROUND_COUNT = 400;

    private static final int FEEDER_THREAD_COUNT = 4;

    private static final String THE_ONE_SESSION_EVERY_FEEDER_THREAD_WRITES = "session-handle-0001";

    private static final String OUTPUT_CARRYING_ONE_CALL =
        "build finished\n<call-to-user>needs approval to deploy</call-to-user>\ndone\n";

    @Test
    public void theSameSessionFedFromEveryThreadAtOnceCallsTheOwnerExactlyOncePerRound()
        throws Exception {
        AtomicInteger ownerCallCount = new AtomicInteger();
        ExecutorService feederThreads = Executors.newFixedThreadPool(FEEDER_THREAD_COUNT);
        try {
            for (int round = 0; round < ROUND_COUNT; round++) {
                CallToUserTagController controller = new CallToUserTagController(
                    (sessionKey, reason, callCycleKey) -> ownerCallCount.incrementAndGet());
                CyclicBarrier everyFeederStartsTogether = new CyclicBarrier(FEEDER_THREAD_COUNT);
                List<Future<Void>> feeds = new ArrayList<>();
                for (int feeder = 0; feeder < FEEDER_THREAD_COUNT; feeder++) {
                    feeds.add(feederThreads.submit((Callable<Void>) () -> {
                        everyFeederStartsTogether.await();
                        controller.onSessionTextChanged(
                            THE_ONE_SESSION_EVERY_FEEDER_THREAD_WRITES, OUTPUT_CARRYING_ONE_CALL);
                        return null;
                    }));
                }
                for (Future<Void> feed : feeds) {
                    feed.get();
                }
            }
        } finally {
            feederThreads.shutdownNow();
        }

        assertEquals("the periodic displayed-session scan now reaches the shared call-to-user "
                + "controller from the statusline parse thread while the service client keeps feeding "
                + "the same session handle from the main thread, so one session's deduplication state "
                + "is written by two threads at once; one transcript carrying one call must call the "
                + "owner exactly once however many threads observe it in the same instant, otherwise "
                + "the owner is called twice for one request or is never called at all",
            ROUND_COUNT, ownerCallCount.get());
    }
}
