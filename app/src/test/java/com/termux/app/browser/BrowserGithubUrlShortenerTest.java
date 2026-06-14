package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserGithubUrlShortenerTest {

    @Test
    public void stripsHttpsGithubPrefixForIssueUrl() {
        Assert.assertEquals(
            "org/repo/issues/123",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo/issues/123"));
    }

    @Test
    public void stripsHttpsGithubPrefixForPullUrl() {
        Assert.assertEquals(
            "org/repo/pull/45",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo/pull/45"));
    }

    @Test
    public void stripsHttpsGithubPrefixForRepoRootUrl() {
        Assert.assertEquals(
            "org/repo",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo"));
    }

    @Test
    public void stripsHttpGithubPrefix() {
        Assert.assertEquals(
            "org/repo/issues/7",
            BrowserGithubUrlShortener.shorten("http://github.com/org/repo/issues/7"));
    }

    @Test
    public void treatsWwwGithubTheSameAsGithub() {
        Assert.assertEquals(
            "org/repo/pull/45",
            BrowserGithubUrlShortener.shorten("https://www.github.com/org/repo/pull/45"));
    }

    @Test
    public void treatsGithubHostCaseInsensitively() {
        Assert.assertEquals(
            "org/repo/issues/9",
            BrowserGithubUrlShortener.shorten("https://GitHub.com/org/repo/issues/9"));
    }

    @Test
    public void dropsTrailingSlash() {
        Assert.assertEquals(
            "org/repo/issues/123",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo/issues/123/"));
    }

    @Test
    public void keepsPathOnlyForBareGithubHostWithTrailingSlash() {
        Assert.assertEquals(
            "github.com",
            BrowserGithubUrlShortener.shorten("https://github.com/"));
    }

    @Test
    public void keepsHostForBareGithubHostWithoutPath() {
        Assert.assertEquals(
            "github.com",
            BrowserGithubUrlShortener.shorten("https://github.com"));
    }

    @Test
    public void keepsQueryAndFragmentAsPartOfPath() {
        Assert.assertEquals(
            "org/repo/issues?q=is%3Aopen",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo/issues?q=is%3Aopen"));
        Assert.assertEquals(
            "org/repo/pull/45#issuecomment-1",
            BrowserGithubUrlShortener.shorten("https://github.com/org/repo/pull/45#issuecomment-1"));
    }

    @Test
    public void leavesNonGithubUrlUnchanged() {
        Assert.assertEquals(
            "https://example.com/org/repo/issues/123",
            BrowserGithubUrlShortener.shorten("https://example.com/org/repo/issues/123"));
        Assert.assertEquals(
            "https://gitlab.com/org/repo",
            BrowserGithubUrlShortener.shorten("https://gitlab.com/org/repo"));
    }

    @Test
    public void doesNotStripGithubSubdomainsThatAreNotTheMainHost() {
        Assert.assertEquals(
            "https://raw.githubusercontent.com/org/repo/main/file",
            BrowserGithubUrlShortener.shorten("https://raw.githubusercontent.com/org/repo/main/file"));
        Assert.assertEquals(
            "https://gist.github.com/org/abc123",
            BrowserGithubUrlShortener.shorten("https://gist.github.com/org/abc123"));
    }

    @Test
    public void trimsSurroundingWhitespace() {
        Assert.assertEquals(
            "org/repo",
            BrowserGithubUrlShortener.shorten("  https://github.com/org/repo  "));
    }

    @Test
    public void returnsEmptyForNullOrBlank() {
        Assert.assertEquals("", BrowserGithubUrlShortener.shorten(null));
        Assert.assertEquals("", BrowserGithubUrlShortener.shorten("   "));
    }
}
