package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PostReconnectStatuslineRescanRetryPlannerTest {

    private static final long[] BACKOFF = {1500L, 3000L, 5000L, 8000L, 12000L};

    private PostReconnectStatuslineRescanRetryPlanner planner() {
        return new PostReconnectStatuslineRescanRetryPlanner(BACKOFF);
    }

    @Test
    public void firstAttemptFiresAtTheOnLoadRescanDelay() {
        Assert.assertEquals(1500L, planner().firstAttemptDelayMillis());
    }

    @Test
    public void schedulesEveryRemainingAttemptWhenStatuslinesNeverParse() {
        PostReconnectStatuslineRescanRetryPlanner planner = planner();
        List<Long> scheduledDelays = new ArrayList<>();
        int nextAttemptIndex = 1;
        while (planner.shouldScheduleNextAttempt(nextAttemptIndex, false)) {
            scheduledDelays.add(planner.delayUntilNextAttemptMillis(nextAttemptIndex));
            nextAttemptIndex++;
        }

        Assert.assertEquals(java.util.Arrays.asList(1500L, 2000L, 3000L, 4000L), scheduledDelays);
        Assert.assertEquals(BACKOFF.length, nextAttemptIndex);
    }

    @Test
    public void totalAttemptsAreBoundedSoTheRetryCannotRunUnbounded() {
        PostReconnectStatuslineRescanRetryPlanner planner = planner();
        int attemptsFired = 1;
        int nextAttemptIndex = 1;
        while (planner.shouldScheduleNextAttempt(nextAttemptIndex, false)) {
            attemptsFired++;
            nextAttemptIndex++;
        }

        Assert.assertEquals(BACKOFF.length, attemptsFired);
        Assert.assertFalse(planner.shouldScheduleNextAttempt(BACKOFF.length, false));
    }

    @Test
    public void stopsEarlyOnceEveryReconnectedSessionHasAParsedStatusline() {
        PostReconnectStatuslineRescanRetryPlanner planner = planner();
        Assert.assertTrue(planner.shouldScheduleNextAttempt(1, false));
        Assert.assertFalse(planner.shouldScheduleNextAttempt(1, true));
        Assert.assertFalse(planner.shouldScheduleNextAttempt(3, true));
    }

    @Test
    public void sessionWhoseStatuslineAppearsOnALaterAttemptStopsTheRetryAtThatAttempt() {
        PostReconnectStatuslineRescanRetryPlanner planner = planner();
        boolean[] parsedAfterScheduledAttemptIndex = {false, false, true, true, true};

        int scheduledAttempts = 0;
        int nextAttemptIndex = 1;
        while (planner.shouldScheduleNextAttempt(
                nextAttemptIndex, parsedAfterScheduledAttemptIndex[nextAttemptIndex - 1])) {
            scheduledAttempts++;
            nextAttemptIndex++;
        }

        Assert.assertEquals(2, scheduledAttempts);
    }

    @Test
    public void noAttemptIsScheduledPastTheEndOfTheBackoffSchedule() {
        PostReconnectStatuslineRescanRetryPlanner planner = planner();
        Assert.assertFalse(planner.hasAttemptAt(BACKOFF.length));
        Assert.assertFalse(planner.shouldScheduleNextAttempt(BACKOFF.length, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestingTheDelayPastTheScheduleEndIsRejected() {
        planner().delayUntilNextAttemptMillis(BACKOFF.length);
    }
}
