package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectedSessionInputReplayRemoteReadinessTest {

    @Test
    public void nothingIsWrittenWhileTheLocalSessionIsNotRunning() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.shouldWriteNow(false, true));
    }

    @Test
    public void nothingIsWrittenWhileTheRemoteTerminalStillTranslatesCarriageReturn() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.shouldWriteNow(true, false));
    }

    @Test
    public void thePayloadIsWrittenOnceTheRemoteTerminalSubmitsCarriageReturn() {
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.shouldWriteNow(true, true));
    }

    @Test
    public void theReplayWindowCoversAnSshAndTmuxAttach() {
        Assert.assertEquals(10_000L, ReconnectedSessionInputReplayPlanner.maxReplayWindowMillis());
    }
}
