package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class FinishedSessionPendingInputTest {

    @Test
    public void consumeReturnsRecordedTypedCharactersInOrder() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();

        for (int codePoint : "echo hi".codePoints().toArray()) {
            pendingInput.recordCodePoint("handle-a", codePoint);
        }

        Assert.assertEquals("echo hi", pendingInput.consume("handle-a"));
    }

    @Test
    public void consumeReturnsEmptyStringWhenNothingRecorded() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();

        Assert.assertEquals("", pendingInput.consume("handle-a"));
    }

    @Test
    public void consumeClearsBufferSoSecondConsumeReturnsEmpty() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();
        pendingInput.recordCodePoint("handle-a", 'x');

        Assert.assertEquals("x", pendingInput.consume("handle-a"));
        Assert.assertEquals("", pendingInput.consume("handle-a"));
    }

    @Test
    public void recordedInputIsKeptSeparatePerSessionHandle() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();
        pendingInput.recordCodePoint("handle-a", 'a');
        pendingInput.recordCodePoint("handle-b", 'b');

        Assert.assertEquals("a", pendingInput.consume("handle-a"));
        Assert.assertEquals("b", pendingInput.consume("handle-b"));
    }

    @Test
    public void discardRemovesRecordedInputForSession() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();
        pendingInput.recordCodePoint("handle-a", 'a');

        pendingInput.discard("handle-a");

        Assert.assertEquals("", pendingInput.consume("handle-a"));
    }

    @Test
    public void recordCodePointIgnoresNegativeCodePoint() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();
        pendingInput.recordCodePoint("handle-a", -1);

        Assert.assertEquals("", pendingInput.consume("handle-a"));
    }

    @Test
    public void recordCodePointStopsBufferingBeyondMaximumLength() {
        FinishedSessionPendingInput pendingInput = new FinishedSessionPendingInput();
        int beyondMaximum = 5000;
        for (int i = 0; i < beyondMaximum; i++) {
            pendingInput.recordCodePoint("handle-a", 'x');
        }

        Assert.assertEquals(4096, pendingInput.consume("handle-a").length());
    }
}
