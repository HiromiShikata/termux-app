package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PendingCallToUserFooterDecisionTest {

    @Test
    public void showsReasonWhenCallToUserIsUnanswered() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, "needs approval");

        assertTrue(decision.isVisible());
        assertEquals("needs approval", decision.getReportText());
    }

    @Test
    public void hidesContentWhenCallToUserHasBeenAnswered() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.NONE, "needs approval");

        assertFalse(decision.isVisible());
        assertEquals("", decision.getReportText());
    }

    @Test
    public void hidesContentForYellowActivityTier() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.YELLOW, "needs approval");

        assertFalse(decision.isVisible());
    }

    @Test
    public void hidesContentWhenReasonIsEmpty() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, "");

        assertFalse(decision.isVisible());
    }

    @Test
    public void hidesContentWhenReasonIsNull() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, null);

        assertFalse(decision.isVisible());
    }

    @Test
    public void hidesContentWhenReasonIsOnlyWhitespace() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, "   ");

        assertFalse(decision.isVisible());
    }

    @Test
    public void resolveAllShowsEveryUnacknowledgedReasonOnItsOwnLine() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED,
                Arrays.asList("needs approval", "deploy failed", "waiting for input"));

        assertTrue(decision.isVisible());
        assertEquals("needs approval\ndeploy failed\nwaiting for input", decision.getReportText());
    }

    @Test
    public void resolveAllShowsSingleReasonWithoutTrailingLineBreak() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED,
                Collections.singletonList("needs approval"));

        assertTrue(decision.isVisible());
        assertEquals("needs approval", decision.getReportText());
    }

    @Test
    public void resolveAllTrimsEachReasonAndSkipsBlankEntries() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED,
                Arrays.asList("  needs approval  ", "", "   ", "deploy failed"));

        assertTrue(decision.isVisible());
        assertEquals("needs approval\ndeploy failed", decision.getReportText());
    }

    @Test
    public void resolveAllHidesContentWhenAllReasonsAreBlank() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED,
                Arrays.asList("", "   "));

        assertFalse(decision.isVisible());
        assertEquals("", decision.getReportText());
    }

    @Test
    public void resolveAllHidesContentWhenReasonsListIsEmpty() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED,
                Collections.emptyList());

        assertFalse(decision.isVisible());
    }

    @Test
    public void resolveAllHidesContentWhenReasonsListIsNull() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.RED, null);

        assertFalse(decision.isVisible());
    }

    @Test
    public void resolveAllHidesContentForNonRedTierEvenWithReasons() {
        PendingCallToUserFooterDecision decision =
            PendingCallToUserFooterDecision.resolveAll(SessionNewActivityTier.YELLOW,
                Arrays.asList("needs approval", "deploy failed"));

        assertFalse(decision.isVisible());
    }
}
