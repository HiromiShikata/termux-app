package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class TmuxMouseModeControllerTest {

    @Test
    public void nextStateTurnsOnWhenCurrentlyOff() {
        Assert.assertTrue(TmuxMouseModeController.nextState(false));
    }

    @Test
    public void nextStateTurnsOffWhenCurrentlyOn() {
        Assert.assertFalse(TmuxMouseModeController.nextState(true));
    }

    @Test
    public void setMouseArgumentsForOnState() {
        Assert.assertArrayEquals(new String[]{"set", "-g", "mouse", "on"},
            TmuxMouseModeController.setMouseArguments(true));
    }

    @Test
    public void setMouseArgumentsForOffState() {
        Assert.assertArrayEquals(new String[]{"set", "-g", "mouse", "off"},
            TmuxMouseModeController.setMouseArguments(false));
    }

    @Test
    public void parseMouseStateReadsOn() {
        Assert.assertEquals(Boolean.TRUE, TmuxMouseModeController.parseMouseState("on\n"));
    }

    @Test
    public void parseMouseStateReadsOff() {
        Assert.assertEquals(Boolean.FALSE, TmuxMouseModeController.parseMouseState("off\n"));
    }

    @Test
    public void parseMouseStateIsNullWhenNoOutput() {
        Assert.assertNull(TmuxMouseModeController.parseMouseState(null));
    }

    @Test
    public void parseMouseStateIsNullWhenUnrecognized() {
        Assert.assertNull(TmuxMouseModeController.parseMouseState(""));
        Assert.assertNull(TmuxMouseModeController.parseMouseState("no server running on socket"));
    }
}
