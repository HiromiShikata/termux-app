package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectedSessionInputReplayPayloadTest {

    @Test
    public void nullPendingInputHasNothingToReplay() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.hasReplayableInput(null));
    }

    @Test
    public void emptyPendingInputHasNothingToReplay() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.hasReplayableInput(""));
    }

    @Test
    public void typedPendingInputIsReplayable() {
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.hasReplayableInput("resume the build"));
    }

    @Test
    public void replayPayloadSubmitsWithACarriageReturnTerminator() {
        Assert.assertEquals("resume the build\r",
            ReconnectedSessionInputReplayPlanner.replayPayload("resume the build"));
    }

    @Test
    public void replayPayloadNeverContainsALineFeed() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.replayPayload("resume the build").contains("\n"));
    }
}
