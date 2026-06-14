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
    public void parseShowMouseOutputReadsOn() {
        Assert.assertTrue(TmuxMouseModeController.parseShowMouseOutput("on\n", false));
    }

    @Test
    public void parseShowMouseOutputReadsOff() {
        Assert.assertFalse(TmuxMouseModeController.parseShowMouseOutput("off\n", true));
    }

    @Test
    public void parseShowMouseOutputFallsBackWhenNull() {
        Assert.assertTrue(TmuxMouseModeController.parseShowMouseOutput(null, true));
        Assert.assertFalse(TmuxMouseModeController.parseShowMouseOutput(null, false));
    }

    @Test
    public void parseShowMouseOutputFallsBackWhenUnrecognized() {
        Assert.assertTrue(TmuxMouseModeController.parseShowMouseOutput("", true));
        Assert.assertFalse(TmuxMouseModeController.parseShowMouseOutput("garbage", false));
    }
}
