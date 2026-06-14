package com.termux.view.url;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowsableUrlDetectorTest {

    @Test
    public void ipv4HostUrlWithPortAndPathIsBrowsable() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl(
            "http://50.30.32.53:9980/in-tmux-by-human/index.v3.json"));
    }

    @Test
    public void ipv4HostUrlWithoutPortIsBrowsable() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("http://127.0.0.1/"));
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("https://192.168.0.1"));
    }

    @Test
    public void normalHostnameUrlIsBrowsable() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl(
            "https://github.com/HiromiShikata/termux-app/issues/224"));
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("http://example.com"));
    }

    @Test
    public void schemelessHostnameRemainsBrowsableForBackwardCompatibility() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("github.com"));
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("www.example.com/path"));
    }

    @Test
    public void ipv6HostUrlWithPortIsBrowsable() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("http://[::1]:8080/path"));
    }

    @Test
    public void surroundingWhitespaceIsToleratedBeforeDetection() {
        Assert.assertTrue(BrowsableUrlDetector.isLikelyBrowsableUrl("  https://example.com/  "));
    }

    @Test
    public void plainTextIsNotBrowsable() {
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("bash"));
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("my project session"));
    }

    @Test
    public void schemeWithoutHostIsNotBrowsable() {
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("http://"));
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("https:///path"));
    }

    @Test
    public void embeddedWhitespaceMakesTextNonBrowsable() {
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("http://example.com/a b"));
    }

    @Test
    public void nullOrEmptyTextIsNotBrowsable() {
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl(null));
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl(""));
        Assert.assertFalse(BrowsableUrlDetector.isLikelyBrowsableUrl("   "));
    }
}
