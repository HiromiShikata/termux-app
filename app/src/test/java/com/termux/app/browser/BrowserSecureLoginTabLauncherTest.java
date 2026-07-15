package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserSecureLoginTabLauncherTest {

    private static final class RecordingSecureTabSink
            implements BrowserSecureLoginTabLauncher.SecureTabSink {

        final List<BrowserSecureLoginTabLaunchMechanism> mechanisms = new ArrayList<>();

        final List<String> openedUrls = new ArrayList<>();

        @Override
        public void openInSecureTab(BrowserSecureLoginTabLaunchMechanism mechanism, String url) {
            mechanisms.add(mechanism);
            openedUrls.add(url);
        }
    }

    private static final class RecordingExternalChromeSink
            implements BrowserSecureLoginTabLauncher.ExternalChromeSink {

        final List<String> openedUrls = new ArrayList<>();

        @Override
        public void openInExternalChrome(String url) {
            openedUrls.add(url);
        }
    }

    private BrowserSecureLoginTabLauncher launcher(
            String trustedUrl,
            BrowserSecureLoginTabLaunchMechanism mechanism,
            RecordingSecureTabSink secureTabSink,
            RecordingExternalChromeSink externalChromeSink) {
        return new BrowserSecureLoginTabLauncher(
            () -> trustedUrl,
            () -> mechanism,
            secureTabSink,
            externalChromeSink);
    }

    @Test
    public void opensTheAppsTrustedCurrentUrlInASecureTabWhenACustomTabProviderIsAvailable() {
        RecordingSecureTabSink secureTabSink = new RecordingSecureTabSink();
        RecordingExternalChromeSink externalChromeSink = new RecordingExternalChromeSink();
        BrowserSecureLoginTabLauncher launcher = launcher(
            "https://legit.example/login",
            BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB,
            secureTabSink, externalChromeSink);

        launcher.openTrustedCurrentUrlInSecureLoginTab();

        Assert.assertEquals(1, secureTabSink.openedUrls.size());
        Assert.assertEquals("https://legit.example/login", secureTabSink.openedUrls.get(0));
        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB, secureTabSink.mechanisms.get(0));
        Assert.assertTrue(externalChromeSink.openedUrls.isEmpty());
    }

    @Test
    public void routesAuthTabMechanismToTheSecureTabSink() {
        RecordingSecureTabSink secureTabSink = new RecordingSecureTabSink();
        RecordingExternalChromeSink externalChromeSink = new RecordingExternalChromeSink();
        BrowserSecureLoginTabLauncher launcher = launcher(
            "https://legit.example/login",
            BrowserSecureLoginTabLaunchMechanism.AUTH_TAB,
            secureTabSink, externalChromeSink);

        launcher.openTrustedCurrentUrlInSecureLoginTab();

        Assert.assertEquals(
            BrowserSecureLoginTabLaunchMechanism.AUTH_TAB, secureTabSink.mechanisms.get(0));
        Assert.assertTrue(externalChromeSink.openedUrls.isEmpty());
    }

    @Test
    public void fallsBackToExternalChromeWhenCustomTabsAreUnavailable() {
        RecordingSecureTabSink secureTabSink = new RecordingSecureTabSink();
        RecordingExternalChromeSink externalChromeSink = new RecordingExternalChromeSink();
        BrowserSecureLoginTabLauncher launcher = launcher(
            "https://legit.example/login",
            BrowserSecureLoginTabLaunchMechanism.EXTERNAL_CHROME,
            secureTabSink, externalChromeSink);

        launcher.openTrustedCurrentUrlInSecureLoginTab();

        Assert.assertEquals(1, externalChromeSink.openedUrls.size());
        Assert.assertEquals("https://legit.example/login", externalChromeSink.openedUrls.get(0));
        Assert.assertTrue(secureTabSink.openedUrls.isEmpty());
    }

    @Test
    public void neverOpensAPageSuppliedForeignUrlOnlyTheTrustedCurrentUrl() {
        RecordingSecureTabSink secureTabSink = new RecordingSecureTabSink();
        RecordingExternalChromeSink externalChromeSink = new RecordingExternalChromeSink();
        String trustedUrl = "https://legit.example/login";
        String attackerUrl = "https://attacker.example/phishing";
        BrowserSecureLoginTabLauncher launcher = launcher(
            trustedUrl,
            BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB,
            secureTabSink, externalChromeSink);

        launcher.openTrustedCurrentUrlInSecureLoginTab();

        Assert.assertFalse(secureTabSink.openedUrls.contains(attackerUrl));
        Assert.assertFalse(externalChromeSink.openedUrls.contains(attackerUrl));
        Assert.assertEquals(1, secureTabSink.openedUrls.size());
        Assert.assertEquals(trustedUrl, secureTabSink.openedUrls.get(0));
    }

    @Test
    public void doesNothingWhenNoTrustedCurrentUrlIsAvailable() {
        RecordingSecureTabSink secureTabSink = new RecordingSecureTabSink();
        RecordingExternalChromeSink externalChromeSink = new RecordingExternalChromeSink();
        BrowserSecureLoginTabLauncher nullSource = launcher(
            null, BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB, secureTabSink, externalChromeSink);
        BrowserSecureLoginTabLauncher emptySource = launcher(
            "", BrowserSecureLoginTabLaunchMechanism.CUSTOM_TAB, secureTabSink, externalChromeSink);

        nullSource.openTrustedCurrentUrlInSecureLoginTab();
        emptySource.openTrustedCurrentUrlInSecureLoginTab();

        Assert.assertTrue(secureTabSink.openedUrls.isEmpty());
        Assert.assertTrue(externalChromeSink.openedUrls.isEmpty());
    }
}
