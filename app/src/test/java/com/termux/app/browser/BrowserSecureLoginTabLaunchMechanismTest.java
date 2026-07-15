package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSecureLoginTabLaunchMechanismTest {

    @Test
    public void prefersAuthTabWhenBothAuthTabApiAndACustomTabsProviderAreAvailable() {
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.AUTH_TAB,
            BrowserSecureLoginTabLaunchMechanism.resolve(true, true));
    }

    @Test
    public void usesCustomTabWhenAProviderExistsButTheAuthTabApiIsNotAvailable() {
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB,
            BrowserSecureLoginTabLaunchMechanism.resolve(false, true));
    }

    @Test
    public void fallsBackToExternalChromeWhenNoCustomTabsProviderIsAvailable() {
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.EXTERNAL_CHROME,
            BrowserSecureLoginTabLaunchMechanism.resolve(false, false));
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.EXTERNAL_CHROME,
            BrowserSecureLoginTabLaunchMechanism.resolve(true, false));
    }
}
