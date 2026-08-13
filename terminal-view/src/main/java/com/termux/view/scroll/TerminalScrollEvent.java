package com.termux.view.scroll;

public enum TerminalScrollEvent {
    MOUSE_WHEEL,
    ARROW_KEY,
    LOCAL_SCROLLBACK;

    public static TerminalScrollEvent ofEmulatorState(boolean mouseTrackingActive, boolean alternateScreenActive,
                                                      boolean fromScrollControl) {
        if (mouseTrackingActive) return MOUSE_WHEEL;
        if (fromScrollControl && alternateScreenActive) return MOUSE_WHEEL;
        if (alternateScreenActive) return ARROW_KEY;
        return LOCAL_SCROLLBACK;
    }
}
