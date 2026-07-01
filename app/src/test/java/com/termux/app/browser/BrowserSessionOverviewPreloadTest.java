package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionOverviewPreloadTest {

    @Test
    public void preloadsOverviewUrlForNonUrlSessionNameWithResolvedOverviewUrl() {
        BrowserProjectActionUrls actionUrls = new BrowserProjectActionUrls(
            "https://github.com/owner/repo/projects/1", "https://console.example/", "https://newissue.example/");

        String preloadUrl = BrowserSessionOverviewPreload.resolvePreloadUrl("project-apm", actionUrls);

        Assert.assertEquals("https://github.com/owner/repo/projects/1", preloadUrl);
    }

    @Test
    public void doesNotPreloadForUrlSessionName() {
        BrowserProjectActionUrls actionUrls = new BrowserProjectActionUrls(
            "https://github.com/owner/repo/projects/1", "https://console.example/", "https://newissue.example/");

        String preloadUrl =
            BrowserSessionOverviewPreload.resolvePreloadUrl("https://github.com/owner/repo/issues/1", actionUrls);

        Assert.assertNull(preloadUrl);
    }

    @Test
    public void doesNotPreloadForNonUrlSessionNameWithoutResolvedOverviewUrl() {
        String preloadUrl =
            BrowserSessionOverviewPreload.resolvePreloadUrl("project-apm", BrowserProjectActionUrls.EMPTY);

        Assert.assertNull(preloadUrl);
    }

    @Test
    public void doesNotPreloadForNullSessionName() {
        BrowserProjectActionUrls actionUrls = new BrowserProjectActionUrls(
            "https://github.com/owner/repo/projects/1", null, null);

        String preloadUrl = BrowserSessionOverviewPreload.resolvePreloadUrl(null, actionUrls);

        Assert.assertEquals("https://github.com/owner/repo/projects/1", preloadUrl);
    }
}
