package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class SendButtonReplySubmitDecisionTest {

    @Test
    public void recordsReplyWhenOwnerContentWasSubmitted() {
        Assert.assertTrue(SendButtonReplySubmitDecision.shouldRecordReply(true));
    }

    @Test
    public void doesNotRecordReplyWhenNoOwnerContentWasSubmitted() {
        Assert.assertFalse(SendButtonReplySubmitDecision.shouldRecordReply(false));
    }
}
