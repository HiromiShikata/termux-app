package com.termux.view.url;

import org.junit.Assert;
import org.junit.Test;

public class DetectedUrlSanitizerTest {

    @Test
    public void percentEncodedJumpToBottomHintIsStripped() {
        String broken = "https://github.com/X-Mile/e-learning-saas/is%20Jump%20to%20bottom%20(ctrl+End)%20%E2%86%93%203546";
        Assert.assertEquals("https://github.com/X-Mile/e-learning-saas/is",
            DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void rawJumpToBottomHintIsStripped() {
        String broken = "https://github.com/X-Mile/e-learning-saas/is Jump to bottom (ctrl+End) ↓ 3546";
        Assert.assertEquals("https://github.com/X-Mile/e-learning-saas/is",
            DetectedUrlSanitizer.sanitize(broken));
    }

    @Test
    public void normalUrlIsReturnedUnchanged() {
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
    public void trailingSentencePunctuationIsTrimmed() {
        Assert.assertEquals("https://example.com/path",
            DetectedUrlSanitizer.sanitize("https://example.com/path."));
    }

    @Test
    public void nullReturnsNull() {
        Assert.assertNull(DetectedUrlSanitizer.sanitize(null));
    }
}
