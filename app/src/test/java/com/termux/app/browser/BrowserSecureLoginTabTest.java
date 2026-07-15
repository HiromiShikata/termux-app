package com.termux.app.browser;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BrowserSecureLoginTabTest {

    @Test
    public void authTabApiIsReportedAvailableWhenTheClassIsOnTheClasspath() {
        Assert.assertTrue(BrowserSecureLoginTab.isAuthTabApiAvailable());
    }

    @Test
    public void resolvesToExternalChromeWhenNoCustomTabsProviderIsInstalled() {
        Context context = RuntimeEnvironment.getApplication();

        Assert.assertFalse(BrowserSecureLoginTab.isCustomTabsProviderAvailable(context));
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.EXTERNAL_CHROME,
            BrowserSecureLoginTab.resolveMechanism(context));
    }
}
