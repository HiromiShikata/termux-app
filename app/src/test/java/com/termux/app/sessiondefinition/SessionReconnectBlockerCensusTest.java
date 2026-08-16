package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SessionReconnectBlockerCensusTest {

    private static final long SCAN_MILLIS = 1783216800000L;

    private static SessionReconnectBlockerCensus.ConsideredSession shellGoneAndWaitingOnTheExitBackoff(
        long remainingMillis) {
        return new SessionReconnectBlockerCensus.ConsideredSession(false, false, false, 0L, false,
            false, remainingMillis, 0L, false);
    }

    private static SessionReconnectBlockerCensus.ConsideredSession shellGoneAndStillMarkedReconnecting(
        long reconnectingForMillis) {
        return new SessionReconnectBlockerCensus.ConsideredSession(false, false, true,
            reconnectingForMillis, false, false, 0L, 0L, false);
    }

    private static SessionReconnectBlockerCensus.ConsideredSession theDisplayedSessionSilentAndOutOfReach() {
        return new SessionReconnectBlockerCensus.ConsideredSession(true, true, false, 0L, false, true,
            0L, 0L, false);
    }

    @Test
    public void aCensusThatNoScanHasFilledInSaysSoRatherThanReadingAsAnEmptyScan() {
        Assert.assertFalse("a census of zero sessions and a census no scan ever took read identically"
                + " once they are printed as counts, and a reader who cannot tell them apart concludes"
                + " that every session was reconnectable when in fact nothing was ever measured",
            SessionReconnectBlockerCensus.NOT_TAKEN.isTaken());
    }

    @Test
    public void aScanThatConsideredNothingIsStillRecordedAsHavingRun() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(
            Collections.<SessionReconnectBlockerCensus.ConsideredSession>emptyList(), SCAN_MILLIS);

        Assert.assertTrue("a scan that found no session to consider is a finding of its own, because it"
            + " means the sessions on screen were outside the set the scan looks at", census.isTaken());
        Assert.assertEquals(0, census.getConsideredCount());
    }

    @Test
    public void aDeadSessionHeldBackByTheExitBackoffIsCountedUnderThatBackoffWithTheLongestWaitLeft() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(Arrays.asList(
            shellGoneAndWaitingOnTheExitBackoff(90_000L),
            shellGoneAndWaitingOnTheExitBackoff(240_000L)), SCAN_MILLIS);

        Assert.assertEquals(2, census.getShellGoneCount());
        Assert.assertEquals(2, census.getShellGoneInsideTheExitBackoffCount());
        Assert.assertEquals("the owner reports a session left unreconnected for over a minute, so the"
                + " remaining wait is the number that says whether this backoff explains it",
            240_000L, census.getLongestExitBackoffRemainingMillis());
        Assert.assertEquals(0, census.getPlannedCount());
    }

    @Test
    public void aDeadSessionStillMarkedReconnectingIsCountedThereRatherThanUnderTheBackoff() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(Arrays.asList(
            shellGoneAndStillMarkedReconnecting(200_000L)), SCAN_MILLIS);

        Assert.assertEquals("a session marked reconnecting is excluded from the plan before any backoff"
                + " is consulted, so counting it under the backoff would send the reader after the wrong"
                + " cause", 1, census.getShellGoneMarkedReconnectingCount());
        Assert.assertEquals(0, census.getShellGoneInsideTheExitBackoffCount());
        Assert.assertEquals("how long the mark has been held is what separates a reconnect in flight"
            + " from one that will never complete", 200_000L, census.getLongestReconnectingMillis());
    }

    @Test
    public void aDeadSessionThatWasReadyAndStillLeftOutOfThePlanIsCountedOnItsOwn() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(Arrays.asList(
            shellGoneAndWaitingOnTheExitBackoff(0L)), SCAN_MILLIS);

        Assert.assertEquals("a session that passed every wait and was still not planned points at the"
                + " reconnect decision itself rather than at any timer, and that distinction is the"
                + " reason this census exists", 1, census.getShellGoneReadyButNotPlannedCount());
    }

    @Test
    public void theDisplayedSessionSkippedByTheSilentPathIsCountedSeparatelyFromTheOthers() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(Arrays.asList(
            theDisplayedSessionSilentAndOutOfReach()), SCAN_MILLIS);

        Assert.assertEquals(1, census.getSilentCount());
        Assert.assertEquals("the session on screen is the one the owner is looking at when he reports"
                + " that nothing reconnects, and it is deliberately excluded from the silent-session"
                + " path, so it has to be visible as its own count",
            1, census.getSilentDisplayedRightNowCount());
        Assert.assertEquals(0, census.getSilentInsideTheSilenceBackoffCount());
    }

    @Test
    public void aPlannedSessionIsCountedAsPlannedRatherThanAsBlocked() {
        SessionReconnectBlockerCensus census = SessionReconnectBlockerCensus.of(Arrays.asList(
            new SessionReconnectBlockerCensus.ConsideredSession(false, false, false, 0L, false, false,
                0L, 0L, true)), SCAN_MILLIS);

        Assert.assertEquals(1, census.getPlannedCount());
        Assert.assertEquals(0, census.getShellGoneReadyButNotPlannedCount());
    }
}
