package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class TerminalEnterKeyEncoderTest {

    private static final String CARRIAGE_RETURN = "\r";

    @Test
    public void sendsCarriageReturnInNormalMode() {
        Assert.assertEquals(CARRIAGE_RETURN, TerminalEnterKeyEncoder.enterSequence(false, false));
    }

    @Test
    public void sendsCarriageReturnInCursorKeysApplicationMode() {
        Assert.assertEquals(CARRIAGE_RETURN, TerminalEnterKeyEncoder.enterSequence(true, false));
    }

    @Test
    public void sendsCarriageReturnInKeypadApplicationMode() {
        Assert.assertEquals(CARRIAGE_RETURN, TerminalEnterKeyEncoder.enterSequence(false, true));
    }

    @Test
    public void sendsCarriageReturnWhenBothApplicationModesEnabled() {
        Assert.assertEquals(CARRIAGE_RETURN, TerminalEnterKeyEncoder.enterSequence(true, true));
    }
}
