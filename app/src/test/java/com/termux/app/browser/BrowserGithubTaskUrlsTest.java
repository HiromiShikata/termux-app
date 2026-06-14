package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserGithubTaskUrlsTest {

    @Test
    public void collectScriptMatchesIssueAndPullRequestUrls() {
        Assert.assertTrue(BrowserGithubTaskUrls.COLLECT_SCRIPT.contains("(issues|pull)"));
        Assert.assertTrue(BrowserGithubTaskUrls.COLLECT_SCRIPT.contains(BrowserGithubTaskUrls.TASK_URL_JS_PATTERN));
    }

    @Test
    public void collectScriptDeduplicatesAndPreservesDomOrder() {
        Assert.assertTrue(BrowserGithubTaskUrls.COLLECT_SCRIPT.contains("new Set()"));
        Assert.assertTrue(BrowserGithubTaskUrls.COLLECT_SCRIPT.contains("getElementsByTagName('a')"));
        Assert.assertTrue(BrowserGithubTaskUrls.COLLECT_SCRIPT.contains("Array.from(uniqueUrls)"));
    }

    @Test
    public void taskUrlJsPatternMatchesIssueUrl() {
        Assert.assertTrue(
            "https://github.com/HiromiShikata/termux-app/issues/272".matches(".*github\\.com/[^/]+/[^/]+/(issues|pull)/\\d+.*"));
    }

    @Test
    public void parseCollectedUrlsReadsJsonArray() throws Exception {
        List<String> urls = BrowserGithubTaskUrls.parseCollectedUrls(
            "[\"https://github.com/o/r/issues/1\",\"https://github.com/o/r/pull/2\"]");
        Assert.assertEquals(
            Arrays.asList("https://github.com/o/r/issues/1", "https://github.com/o/r/pull/2"),
            urls);
    }

    @Test
    public void parseCollectedUrlsReturnsEmptyForNullResult() throws Exception {
        Assert.assertTrue(BrowserGithubTaskUrls.parseCollectedUrls(null).isEmpty());
        Assert.assertTrue(BrowserGithubTaskUrls.parseCollectedUrls("null").isEmpty());
        Assert.assertTrue(BrowserGithubTaskUrls.parseCollectedUrls("[]").isEmpty());
    }

    @Test
    public void selectForBulkOpenReturnsAllWhenLimitIsZero() {
        List<String> urls = Arrays.asList("a", "b", "c");
        Assert.assertEquals(urls, BrowserGithubTaskUrls.selectForBulkOpen(urls, 0));
    }

    @Test
    public void selectForBulkOpenReturnsAllWhenLimitExceedsSize() {
        List<String> urls = Arrays.asList("a", "b", "c");
        Assert.assertEquals(urls, BrowserGithubTaskUrls.selectForBulkOpen(urls, 10));
    }

    @Test
    public void selectForBulkOpenReturnsFirstNInOrder() {
        List<String> urls = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Assert.assertEquals(Arrays.asList("a", "b"), BrowserGithubTaskUrls.selectForBulkOpen(urls, 2));
    }

    @Test
    public void openFirstNLimitIsTen() {
        Assert.assertEquals(10, BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT);
    }
}
