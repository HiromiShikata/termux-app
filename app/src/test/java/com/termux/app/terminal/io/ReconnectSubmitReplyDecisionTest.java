package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectSubmitReplyDecisionTest {

    @Test
    public void ownerContentDeliveredIntoTheReconnectedSessionIsAnOwnerReply() {
        Assert.assertTrue(ReconnectSubmitReplyDecision.shouldRecordReply(true, "ok, go ahead"));
    }

    @Test
    public void anEmptySubmitCarriesNoOwnerContentAndIsNotAnOwnerReply() {
        Assert.assertFalse(ReconnectSubmitReplyDecision.shouldRecordReply(true, ""));
    }

    @Test
    public void contentThatNeverReachedASessionBecauseTheReconnectFailedIsNotAnOwnerReply() {
        Assert.assertFalse(ReconnectSubmitReplyDecision.shouldRecordReply(false, "ok, go ahead"));
    }
}
