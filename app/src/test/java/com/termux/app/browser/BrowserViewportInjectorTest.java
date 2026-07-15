package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserViewportInjectorTest {

    @Test
    public void desktopModeSelectsTheDesktopViewportScript() {
        Assert.assertEquals(
            BrowserDesktopViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.DESKTOP, false));
    }

    @Test
    public void desktopModeSelectsTheDesktopViewportScriptEvenWhenMobileInjectionRequested() {
        Assert.assertEquals(
            BrowserDesktopViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.DESKTOP, true));
    }

    @Test
    public void mobileModeInjectsNothingWhenMobileViewportInjectionIsDisabled() {
        Assert.assertNull(BrowserViewportInjector.scriptFor(BrowserViewMode.MOBILE, false));
    }

    @Test
    public void mobileModeSelectsTheMobileViewportScriptWhenInjectionIsEnabled() {
        Assert.assertEquals(
            BrowserMobileViewport.INJECTION_SCRIPT,
            BrowserViewportInjector.scriptFor(BrowserViewMode.MOBILE, true));
    }
}
