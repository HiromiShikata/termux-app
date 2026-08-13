package com.termux.view.scroll;

import org.junit.Assert;
import org.junit.Test;

public class TerminalScrollEventOfEmulatorStateTest {

    @Test
    public void aFingerGestureReachesTheViewOnlyWhenNeitherModeIsActive() {
        Assert.assertEquals(TerminalScrollEvent.LOCAL_SCROLLBACK,
            TerminalScrollEvent.ofEmulatorState(false, false, false));
    }

    @Test
    public void theAlternateScreenTurnsAFingerGestureIntoArrowKeysForTheShell() {
        Assert.assertEquals(TerminalScrollEvent.ARROW_KEY,
            TerminalScrollEvent.ofEmulatorState(false, true, false));
    }

    @Test
    public void mouseTrackingTurnsEveryGestureIntoAMouseWheelForTheShell() {
        Assert.assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            TerminalScrollEvent.ofEmulatorState(true, false, false));
        Assert.assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            TerminalScrollEvent.ofEmulatorState(true, true, false));
        Assert.assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            TerminalScrollEvent.ofEmulatorState(true, false, true));
        Assert.assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            TerminalScrollEvent.ofEmulatorState(true, true, true));
    }

    @Test
    public void theScrollControlSendsAMouseWheelOnTheAlternateScreenAndScrollsTheViewOtherwise() {
        Assert.assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            TerminalScrollEvent.ofEmulatorState(false, true, true));
        Assert.assertEquals(TerminalScrollEvent.LOCAL_SCROLLBACK,
            TerminalScrollEvent.ofEmulatorState(false, false, true));
    }

    @Test
    public void theScrollPathAndAnyOtherCallerAgreeOnEveryCombination() {
        TerminalScrollController controller = new TerminalScrollController(12, 4, 40);
        boolean[] values = {false, true};
        for (boolean mouseTrackingActive : values) {
            for (boolean alternateScreenActive : values) {
                for (boolean fromScrollControl : values) {
                    Assert.assertEquals("the report must describe the same routing the scroll path takes,"
                            + " so both have to come from one decision",
                        controller.scrollEventType(mouseTrackingActive, alternateScreenActive, fromScrollControl),
                        TerminalScrollEvent.ofEmulatorState(mouseTrackingActive, alternateScreenActive, fromScrollControl));
                }
            }
        }
    }
}
