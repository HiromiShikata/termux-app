package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BrowserNewTabSessionHandleTest {

    @Test
    public void selectedSessionHandleIsUsedWhenPresent() {
        assertEquals("selected-handle",
            BrowserNewTabSessionHandle.resolve("selected-handle", "displayed-handle"));
    }

    @Test
    public void displayedTerminalSessionHandleIsUsedWhenNoSessionIsSelected() {
        assertEquals("displayed-handle",
            BrowserNewTabSessionHandle.resolve(null, "displayed-handle"));
    }

    @Test
    public void selectedSessionHandleIsUsedWhenNoTerminalSessionIsDisplayed() {
        assertEquals("selected-handle",
            BrowserNewTabSessionHandle.resolve("selected-handle", null));
    }

    @Test
    public void noHandleIsResolvedWhenNoSessionIsSelectedAndNoneIsDisplayed() {
        assertNull(BrowserNewTabSessionHandle.resolve(null, null));
    }
}
