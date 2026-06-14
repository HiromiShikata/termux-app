package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPageHeaderTextTest {

    @Test
    public void joinsProjectAndSessionWithMiddleDotOnContextLine() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "MyProject", "MySession", "Example Domain", "https://example.com/");
        Assert.assertEquals("MyProject · MySession", header.getContextLine());
        Assert.assertEquals("Example Domain", header.getTitleLine());
        Assert.assertEquals("https://example.com/", header.getCompactUrlLine());
    }

    @Test
    public void usesSessionNameAloneWhenProjectIsMissing() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            null, "MySession", "Example Domain", "https://example.com/");
        Assert.assertEquals("MySession", header.getContextLine());
        Assert.assertTrue(header.hasContextLine());
    }

    @Test
    public void usesSessionNameAloneWhenProjectIsBlank() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "   ", "MySession", "Example Domain", "https://example.com/");
        Assert.assertEquals("MySession", header.getContextLine());
    }

    @Test
    public void neverRendersTheLiteralNullForMissingProject() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            null, "MySession", "Title", "https://example.com/");
        Assert.assertFalse(header.getContextLine().contains("null"));
    }

    @Test
    public void contextLineIsEmptyWhenNeitherProjectNorSessionPresent() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            null, null, "Title", "https://example.com/");
        Assert.assertEquals("", header.getContextLine());
        Assert.assertFalse(header.hasContextLine());
    }

    @Test
    public void shortensGithubUrlOnCompactUrlLine() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "MyProject", "MySession", "Issue title",
            "https://github.com/org/repo/issues/123");
        Assert.assertEquals("org/repo/issues/123", header.getCompactUrlLine());
        Assert.assertEquals("Issue title", header.getTitleLine());
    }

    @Test
    public void leavesNonGithubUrlUnchangedOnCompactUrlLine() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "MyProject", "MySession", "Example Domain", "https://example.com/path");
        Assert.assertEquals("https://example.com/path", header.getCompactUrlLine());
    }

    @Test
    public void dropsTitleLineWhenTitleEqualsCompactUrl() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            null, "MySession", "https://example.com/", "https://example.com/");
        Assert.assertEquals("", header.getTitleLine());
        Assert.assertFalse(header.hasTitleLine());
        Assert.assertEquals("https://example.com/", header.getCompactUrlLine());
    }

    @Test
    public void trimsSurroundingWhitespaceOnTitleAndContext() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "  MyProject  ", "  MySession  ", "  Example  ", "  https://example.com/  ");
        Assert.assertEquals("MyProject · MySession", header.getContextLine());
        Assert.assertEquals("Example", header.getTitleLine());
        Assert.assertEquals("https://example.com/", header.getCompactUrlLine());
    }

    @Test
    public void emptyUrlProducesEmptyCompactUrlLine() {
        BrowserPageHeader header = BrowserPageHeaderText.build(
            "MyProject", "MySession", "Title", null);
        Assert.assertEquals("", header.getCompactUrlLine());
        Assert.assertFalse(header.hasCompactUrlLine());
    }
}
