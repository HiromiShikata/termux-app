package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public class DownloadSlotWaiterTest {

    private static final class FakeClock implements LongSupplier {
        private long nowMillis;

        @Override
        public long getAsLong() {
            return nowMillis;
        }

        void advance(long millis) {
            nowMillis += millis;
        }
    }

    private static final class FakeSleeper implements DownloadSlotWaiter.Sleeper {
        private final FakeClock clock;
        private final List<Long> sleptMillis = new ArrayList<>();

        FakeSleeper(FakeClock clock) {
            this.clock = clock;
        }

        @Override
        public void sleep(long millis) {
            sleptMillis.add(millis);
            clock.advance(millis);
        }
    }

    private static final class ScriptedBusy implements BooleanSupplier {
        private final boolean[] outcomes;
        private int invocations;

        ScriptedBusy(boolean[] outcomes) {
            this.outcomes = outcomes;
        }

        @Override
        public boolean getAsBoolean() {
            int index = Math.min(invocations, outcomes.length - 1);
            invocations++;
            return outcomes[index];
        }
    }

    private static final class WaitCase {
        final String name;
        final boolean[] busyOutcomes;
        final long timeoutMillis;
        final boolean expectedIdle;
        final List<Long> expectedSleeps;

        WaitCase(String name, boolean[] busyOutcomes, long timeoutMillis, boolean expectedIdle,
                 List<Long> expectedSleeps) {
            this.name = name;
            this.busyOutcomes = busyOutcomes;
            this.timeoutMillis = timeoutMillis;
            this.expectedIdle = expectedIdle;
            this.expectedSleeps = expectedSleeps;
        }
    }

    @Test
    public void awaitIdleWaitsInHalfSecondStepsUntilIdleOrTimeout() throws InterruptedException {
        WaitCase[] cases = {
            new WaitCase("idle at start returns true without sleeping",
                new boolean[] {false}, 10_000L, true, Collections.<Long>emptyList()),
            new WaitCase("busy for three checks then idle returns true after three sleeps",
                new boolean[] {true, true, true, false}, 10_000L, true, Arrays.asList(500L, 500L, 500L)),
            new WaitCase("always busy returns false once the timeout is reached",
                new boolean[] {true}, 1_000L, false, Arrays.asList(500L, 500L)),
            new WaitCase("zero timeout while busy returns false without sleeping",
                new boolean[] {true}, 0L, false, Collections.<Long>emptyList()),
        };
        for (WaitCase testCase : cases) {
            FakeClock clock = new FakeClock();
            FakeSleeper sleeper = new FakeSleeper(clock);
            DownloadSlotWaiter waiter = new DownloadSlotWaiter(
                new ScriptedBusy(testCase.busyOutcomes), sleeper, clock, testCase.timeoutMillis);

            Assert.assertEquals(testCase.name, testCase.expectedIdle, waiter.awaitIdle());
            Assert.assertEquals(testCase.name, testCase.expectedSleeps, sleeper.sleptMillis);
        }
    }
}
