package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HungSessionReconnectBackoffTest {

    private static final String SESSION_NAME = "quiet-session";

    private static final long START_MILLIS = 1_000_000L;

    @Test
    public void theFirstAttemptAgainstASilenceIsAllowedImmediately() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();

        Assert.assertTrue("a session that has never been attempted for this silence must be reconnected"
                + " without waiting, otherwise a genuinely stuck session is left alone",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L, START_MILLIS));
    }

    @Test
    public void theSameSilenceIsNotAttemptedAgainOnTheFollowingScan() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);

        Assert.assertFalse("reconnecting did not make the recorded output any newer, so repeating it one"
                + " scan later rebuilds a working connection for nothing",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L, START_MILLIS + 60_000L));
    }

    @Test
    public void theSameSilenceIsAttemptedAgainOnceTheShortestWaitHasPassed() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);

        Assert.assertTrue("a session that is still silent much later is still a candidate, because the"
                + " point is to space the repeats rather than to abandon the session",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L,
                START_MILLIS + HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void eachFurtherAttemptAgainstTheSameSilenceWaitsLonger() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);
        long secondAttemptMillis = START_MILLIS + HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS;
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, secondAttemptMillis);

        Assert.assertFalse("the wait after the second attempt is longer than the wait after the first,"
                + " so a session that never speaks costs less and less",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L,
                secondAttemptMillis + HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS - 1L));
        Assert.assertTrue("the doubled wait is still finite",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L,
                secondAttemptMillis + 2L * HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void theWaitStopsGrowingAtTheLongestWait() {
        Assert.assertEquals(0L, HungSessionReconnectBackoff.waitMillis(0));
        Assert.assertEquals(HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS,
            HungSessionReconnectBackoff.waitMillis(1));
        Assert.assertEquals(2L * HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS,
            HungSessionReconnectBackoff.waitMillis(2));
        Assert.assertEquals(4L * HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS,
            HungSessionReconnectBackoff.waitMillis(3));
        Assert.assertEquals("an unbounded doubling would eventually stop attempting a session at all",
            HungSessionReconnectBackoff.LONGEST_WAIT_MILLIS, HungSessionReconnectBackoff.waitMillis(9));
        Assert.assertEquals(HungSessionReconnectBackoff.LONGEST_WAIT_MILLIS,
            HungSessionReconnectBackoff.waitMillis(40));
    }

    @Test
    public void outputFromTheSessionMakesItImmediatelyEligibleAgain() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);

        Assert.assertTrue("the session spoke, so a later silence is a new failure and must not inherit"
                + " the spacing earned by the previous one",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 900L, START_MILLIS + 1L));
    }

    @Test
    public void theLadderRestartsFromTheShortestWaitAfterTheSessionSpeaks() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);
        backoff.recordAttemptForSilence(SESSION_NAME, 500L,
            START_MILLIS + HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS);
        long newSilenceMillis = START_MILLIS + 10_000_000L;
        backoff.recordAttemptForSilence(SESSION_NAME, 900L, newSilenceMillis);

        Assert.assertTrue("after the session speaks the count starts again, so the next failure waits the"
                + " shortest wait rather than the doubled one",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 900L,
                newSilenceMillis + HungSessionReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void aSessionThatIsNoLongerPresentLeavesNoAttemptHistoryBehind() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);

        backoff.forgetSessionsOtherThan(Collections.singleton("another-session"));

        Assert.assertTrue("holding attempt history for sessions that no longer exist grows without bound"
                + " over the life of the process",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L, START_MILLIS + 1L));
    }

    @Test
    public void aSessionThatIsStillPresentKeepsItsAttemptHistory() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, 500L, START_MILLIS);
        Set<String> stillPresent = new HashSet<>();
        stillPresent.add(SESSION_NAME);
        stillPresent.add("another-session");

        backoff.forgetSessionsOtherThan(stillPresent);

        Assert.assertFalse("dropping the history of a session that is still there would restore the loop"
                + " this exists to prevent",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L, START_MILLIS + 60_000L));
    }

    @Test
    public void aSessionWithNoRecordedOutputTimeIsTreatedAsOneSilence() {
        HungSessionReconnectBackoff backoff = new HungSessionReconnectBackoff();
        backoff.recordAttemptForSilence(SESSION_NAME, null, START_MILLIS);

        Assert.assertFalse("an absent output time is a value like any other and must not read as a"
                + " different silence on every scan",
            backoff.isReadyToAttemptAgain(SESSION_NAME, null, START_MILLIS + 60_000L));
        Assert.assertTrue("a recorded output time is a different silence from having none",
            backoff.isReadyToAttemptAgain(SESSION_NAME, 500L, START_MILLIS + 60_000L));
    }
}
