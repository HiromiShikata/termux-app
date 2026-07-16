package com.termux.view.url;

import org.junit.Assert;
import org.junit.Test;

public class DetectedUrlSanitizerTest {

    @Test
    public void rawJumpToBottomHintReturnsNull() {
        String broken = "https://github.com/xcare-medica Jump to bottom (ctrl+End) ↓ 81";
        Assert.assertNull(DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void percentEncodedJumpToBottomHintReturnsNull() {
        String broken = "https://github.com/xcare-medica%20Jump%20to%20bottom%20(ctrl+End)%20%E2%86%93%2081";
        Assert.assertNull(DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void downArrowMarkerReturnsNull() {
        String broken = "https://github.com/HiromiShikata/termux-app/issues/1↓";
        Assert.assertNull(DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void ctrlEndMarkerIsCaseInsensitive() {
        String broken = "https://github.com/HiromiShikata/termux-app (CTRL+END)";
        Assert.assertNull(DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void normalUrlIsReturnedUnchanged() {
        String url = "https://github.com/HiromiShikata/termux-app/issues/1";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void releaseTagUrlIsReturnedUnchanged() {
        String url = "https://github.com/HiromiShikata/termux-app/releases/tag/v0.119.2793";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void queryAndFragmentArePreserved() {
        String url = "https://github.com/HiromiShikata/termux-app/issues?q=is%3Aopen#fragment";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void legitimatePercentEncodedSpaceInQueryIsPreserved() {
        String url = "https://example.com/search?q=hello%20world";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void legitimatePercentEncodedSpaceInMultipleParamsIsPreserved() {
        String url = "https://example.com/path?name=John%20Doe&x=1";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void normalUrlFollowedByUnrelatedTextIsTruncatedAtSpace() {
        String candidate = "https://example.com/path see this later";
        Assert.assertEquals("https://example.com/path",
            DetectedUrlSanitizer.sanitize(candidate));
    }

    @Test
    public void trailingSentencePunctuationIsTrimmed() {
        Assert.assertEquals("https://example.com/path",
            DetectedUrlSanitizer.sanitize("https://example.com/path."));
    }

    @Test
    public void fullWidthTextAdjacentToUrlIsTruncatedBeforeTheFullWidthCharacters() {
        String candidate = "https://example.com/owner/repo/pull/123（説明文がここに続く）";
        Assert.assertEquals("https://example.com/owner/repo/pull/123",
            DetectedUrlSanitizer.sanitize(candidate));
    }

    @Test
    public void fullWidthParenthesisImmediatelyAfterUrlIsTruncated() {
        String candidate = "https://example.com/path（";
        Assert.assertEquals("https://example.com/path",
            DetectedUrlSanitizer.sanitize(candidate));
    }

    @Test
    public void cjkTextImmediatelyAfterUrlIsTruncated() {
        String candidate = "https://example.com/path説明";
        Assert.assertEquals("https://example.com/path",
            DetectedUrlSanitizer.sanitize(candidate));
    }

    @Test
    public void asciiQueryAndFragmentAreNotTruncatedByNonAsciiRule() {
        String url = "https://example.com/search?q=hello+world&x=1#section";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url));
    }

    @Test
    public void nullReturnsNull() {
        Assert.assertNull(DetectedUrlSanitizer.sanitize(null));
    }

    @Test
    public void cleanTokenOnHintRowIsExcluded() {
        String cleanPrefix = "https://github.com/X-Mile/e-learning-sa";
        String lastRowText = "https://github.com/X-Mile/e-learning-sa Jump to bottom (ctrl+End) ↓";
        Assert.assertNull(DetectedUrlSanitizer.sanitize(cleanPrefix, lastRowText));
    }

    @Test
    public void cleanTokenWithNullHintScopeIsReturnedUnchanged() {
        String url = "https://github.com/X-Mile/e-learning-saas";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url, null));
    }

    @Test
    public void cleanTokenOnRowWithoutHintIsReturnedUnchanged() {
        String url = "https://github.com/X-Mile/e-learning-saas";
        String lastRowText = "$ echo https://github.com/X-Mile/e-learning-saas";
        Assert.assertEquals(url, DetectedUrlSanitizer.sanitize(url, lastRowText));
    }
}
