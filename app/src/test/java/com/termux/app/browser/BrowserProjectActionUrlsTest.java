package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserProjectActionUrlsTest {

    @Test
    public void exposesProvidedUrls() {
        BrowserProjectActionUrls actionUrls = new BrowserProjectActionUrls(
            "https://overview.example/", "https://console.example/", "https://newissue.example/");

        Assert.assertEquals("https://overview.example/", actionUrls.getOverviewUrl());
        Assert.assertEquals("https://console.example/", actionUrls.getTdpmConsoleUrl());
        Assert.assertEquals("https://newissue.example/", actionUrls.getNewIssueUrl());
    }

    @Test
    public void normalizesEmptyStringsToNull() {
        BrowserProjectActionUrls actionUrls = new BrowserProjectActionUrls("", null, "");

        Assert.assertNull(actionUrls.getOverviewUrl());
        Assert.assertNull(actionUrls.getTdpmConsoleUrl());
        Assert.assertNull(actionUrls.getNewIssueUrl());
    }

    @Test
    public void emptyConstantHasNoUrls() {
        Assert.assertNull(BrowserProjectActionUrls.EMPTY.getOverviewUrl());
        Assert.assertNull(BrowserProjectActionUrls.EMPTY.getTdpmConsoleUrl());
        Assert.assertNull(BrowserProjectActionUrls.EMPTY.getNewIssueUrl());
    }
}
