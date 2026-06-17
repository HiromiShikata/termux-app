package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserHttpAuthPromptTest {

    @Test
    public void combinesHostAndRealm() {
        Assert.assertEquals(
            "example.com (Restricted Area)",
            BrowserHttpAuthPrompt.describe("example.com", "Restricted Area"));
    }

    @Test
    public void usesHostOnlyWhenRealmMissing() {
        Assert.assertEquals(
            "example.com",
            BrowserHttpAuthPrompt.describe("example.com", null));
    }

    @Test
    public void usesHostOnlyWhenRealmBlank() {
        Assert.assertEquals(
            "example.com",
            BrowserHttpAuthPrompt.describe("example.com", "   "));
    }

    @Test
    public void usesRealmOnlyWhenHostMissing() {
        Assert.assertEquals(
            "Restricted Area",
            BrowserHttpAuthPrompt.describe(null, "Restricted Area"));
    }

    @Test
    public void returnsEmptyWhenBothMissing() {
        Assert.assertEquals("", BrowserHttpAuthPrompt.describe(null, null));
    }

    @Test
    public void trimsSurroundingWhitespace() {
        Assert.assertEquals(
            "example.com (Restricted Area)",
            BrowserHttpAuthPrompt.describe("  example.com  ", "  Restricted Area  "));
    }
}
