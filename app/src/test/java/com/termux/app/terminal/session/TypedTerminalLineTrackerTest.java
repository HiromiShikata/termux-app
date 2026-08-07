package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class TypedTerminalLineTrackerTest {

    private static final String SESSION_HANDLE = "session-handle";
    private static final String OTHER_SESSION_HANDLE = "other-session-handle";

    @Test
    public void aLineTheOwnerTypedIsReportedAsSubmittedOnce() {
        TypedTerminalLineTracker tracker = new TypedTerminalLineTracker();
        tracker.recordCodePoint(SESSION_HANDLE, 'y');
        Assert.assertTrue(tracker.consumeTypedLine(SESSION_HANDLE));
        Assert.assertFalse(tracker.consumeTypedLine(SESSION_HANDLE));
    }

    @Test
    public void anEnterWithNothingTypedIsNotASubmittedLine() {
        TypedTerminalLineTracker tracker = new TypedTerminalLineTracker();
        Assert.assertFalse(tracker.consumeTypedLine(SESSION_HANDLE));
    }

    @Test
    public void aControlCharacterAloneIsNotTypedContent() {
        TypedTerminalLineTracker tracker = new TypedTerminalLineTracker();
        tracker.recordCodePoint(SESSION_HANDLE, 0x03);
        tracker.recordCodePoint(SESSION_HANDLE, 0x7F);
        Assert.assertFalse(tracker.consumeTypedLine(SESSION_HANDLE));
    }

    @Test
    public void whatTheOwnerTypedInOneSessionIsNotASubmittedLineInAnother() {
        TypedTerminalLineTracker tracker = new TypedTerminalLineTracker();
        tracker.recordCodePoint(SESSION_HANDLE, 'y');
        Assert.assertFalse(tracker.consumeTypedLine(OTHER_SESSION_HANDLE));
        Assert.assertTrue(tracker.consumeTypedLine(SESSION_HANDLE));
    }

    @Test
    public void theCarriageReturnAndTheNewlineBothEndATypedLine() {
        Assert.assertTrue(TypedTerminalLineTracker.isLineEndCodePoint('\r'));
        Assert.assertTrue(TypedTerminalLineTracker.isLineEndCodePoint('\n'));
        Assert.assertFalse(TypedTerminalLineTracker.isLineEndCodePoint('y'));
    }
}
