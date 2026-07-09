package com.termux.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RemoteRepaintWindowSizeNudgeTest {

    @Test
    public void nudgesRowsDownByOneThenRestoresForATypicalSize() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(80, 24);
        assertTrue(nudge.shouldNudge());
        assertEquals(23, nudge.getNudgedRows());
        assertEquals(24, nudge.getRestoredRows());
    }

    @Test
    public void nudgedSizeDiffersFromRestoredSizeSoTheKernelRaisesSigwinch() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(80, 24);
        assertNotEquals(nudge.getNudgedRows(), nudge.getRestoredRows());
    }

    @Test
    public void restoredRowsEqualTheOriginalRowsSoTheVisibleSizeIsUnchangedAfterTheRoundTrip() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(120, 40);
        assertEquals(40, nudge.getRestoredRows());
    }

    @Test
    public void singleRowSizeDoesNotNudgeBecauseRowsCanNotGoBelowOne() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(80, 1);
        assertFalse(nudge.shouldNudge());
    }

    @Test
    public void zeroRowsDoesNotNudge() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(80, 0);
        assertFalse(nudge.shouldNudge());
    }

    @Test
    public void zeroColumnsDoesNotNudge() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(0, 24);
        assertFalse(nudge.shouldNudge());
    }

    @Test
    public void negativeSizeDoesNotNudge() {
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(-5, -5);
        assertFalse(nudge.shouldNudge());
    }
}
