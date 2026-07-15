package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserPasskeyOpenInChromeActionTest {

    private static final class RecordingSink
            implements BrowserPasskeyOpenInChromeAction.OpenInChromeSink {

        final List<String> openedUrls = new ArrayList<>();

        @Override
        public void openInChrome(String url) {
            openedUrls.add(url);
        }
    }

    @Test
    public void opensTheAppsTrustedCurrentUrlInChrome() {
        RecordingSink sink = new RecordingSink();
        BrowserPasskeyOpenInChromeAction action = new BrowserPasskeyOpenInChromeAction(
            () -> "https://legit.example/login", sink);

        action.openTrustedCurrentUrlInChrome();

        Assert.assertEquals(1, sink.openedUrls.size());
        Assert.assertEquals("https://legit.example/login", sink.openedUrls.get(0));
    }

    @Test
    public void neverOpensAPageSuppliedForeignUrlOnlyTheTrustedCurrentUrl() {
        RecordingSink sink = new RecordingSink();
        String trustedUrl = "https://legit.example/login";
        String attackerUrl = "https://attacker.example/phishing";
        BrowserPasskeyOpenInChromeAction action = new BrowserPasskeyOpenInChromeAction(
            () -> trustedUrl, sink);

        action.openTrustedCurrentUrlInChrome();

        Assert.assertFalse(sink.openedUrls.contains(attackerUrl));
        Assert.assertEquals(1, sink.openedUrls.size());
        Assert.assertEquals(trustedUrl, sink.openedUrls.get(0));
    }

    @Test
    public void doesNothingWhenNoTrustedCurrentUrlIsAvailable() {
        RecordingSink sink = new RecordingSink();
        BrowserPasskeyOpenInChromeAction nullSource =
            new BrowserPasskeyOpenInChromeAction(() -> null, sink);
        BrowserPasskeyOpenInChromeAction emptySource =
            new BrowserPasskeyOpenInChromeAction(() -> "", sink);

        nullSource.openTrustedCurrentUrlInChrome();
        emptySource.openTrustedCurrentUrlInChrome();

        Assert.assertTrue(sink.openedUrls.isEmpty());
    }
}
