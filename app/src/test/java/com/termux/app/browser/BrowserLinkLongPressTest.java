package com.termux.app.browser;

import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Test;

public class BrowserLinkLongPressTest {

    @Test
    public void anchorHitIsLinkHit() {
        Assert.assertTrue(BrowserLinkLongPress.isLinkHit(WebView.HitTestResult.SRC_ANCHOR_TYPE));
    }

    @Test
    public void imageAnchorHitIsLinkHit() {
        Assert.assertTrue(BrowserLinkLongPress.isLinkHit(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE));
    }

    @Test
    public void plainImageHitIsNotLinkHit() {
        Assert.assertFalse(BrowserLinkLongPress.isLinkHit(WebView.HitTestResult.IMAGE_TYPE));
    }

    @Test
    public void editTextHitIsNotLinkHit() {
        Assert.assertFalse(BrowserLinkLongPress.isLinkHit(WebView.HitTestResult.EDIT_TEXT_TYPE));
    }

    @Test
    public void unknownHitIsNotLinkHit() {
        Assert.assertFalse(BrowserLinkLongPress.isLinkHit(WebView.HitTestResult.UNKNOWN_TYPE));
    }

    @Test
    public void imageAnchorHitRequiresHrefLookup() {
        Assert.assertTrue(BrowserLinkLongPress.requiresHrefLookup(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE));
    }

    @Test
    public void anchorHitDoesNotRequireHrefLookup() {
        Assert.assertFalse(BrowserLinkLongPress.requiresHrefLookup(WebView.HitTestResult.SRC_ANCHOR_TYPE));
    }

    @Test
    public void httpsUrlIsOpenable() {
        Assert.assertTrue(BrowserLinkLongPress.isOpenableLinkUrl("https://example.com/page"));
    }

    @Test
    public void httpUrlIsOpenable() {
        Assert.assertTrue(BrowserLinkLongPress.isOpenableLinkUrl("http://example.com/page"));
    }

    @Test
    public void surroundingWhitespaceDoesNotBlockOpenableUrl() {
        Assert.assertTrue(BrowserLinkLongPress.isOpenableLinkUrl("  https://example.com/  "));
    }

    @Test
    public void nullUrlIsNotOpenable() {
        Assert.assertFalse(BrowserLinkLongPress.isOpenableLinkUrl(null));
    }

    @Test
    public void emptyUrlIsNotOpenable() {
        Assert.assertFalse(BrowserLinkLongPress.isOpenableLinkUrl(""));
    }

    @Test
    public void javascriptUrlIsNotOpenable() {
        Assert.assertFalse(BrowserLinkLongPress.isOpenableLinkUrl("javascript:void(0)"));
    }

    @Test
    public void mailtoUrlIsNotOpenable() {
        Assert.assertFalse(BrowserLinkLongPress.isOpenableLinkUrl("mailto:user@example.com"));
    }

    @Test
    public void anchorWithHttpsExtraIsCopyableLink() {
        Assert.assertTrue(BrowserLinkLongPress.isCopyableLink(
            WebView.HitTestResult.SRC_ANCHOR_TYPE, "https://example.com/page"));
    }

    @Test
    public void anchorWithNonHttpExtraIsNotCopyableLink() {
        Assert.assertFalse(BrowserLinkLongPress.isCopyableLink(
            WebView.HitTestResult.SRC_ANCHOR_TYPE, "mailto:user@example.com"));
    }

    @Test
    public void anchorWithNullExtraIsNotCopyableLink() {
        Assert.assertFalse(BrowserLinkLongPress.isCopyableLink(
            WebView.HitTestResult.SRC_ANCHOR_TYPE, null));
    }

    @Test
    public void imageAnchorIsNotCopyableLinkBecauseExtraIsNotTheHref() {
        Assert.assertFalse(BrowserLinkLongPress.isCopyableLink(
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE, "https://example.com/image.png"));
    }

    @Test
    public void nonLinkHitIsNotCopyableLink() {
        Assert.assertFalse(BrowserLinkLongPress.isCopyableLink(
            WebView.HitTestResult.EDIT_TEXT_TYPE, "https://example.com/page"));
    }
}
