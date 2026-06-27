package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
