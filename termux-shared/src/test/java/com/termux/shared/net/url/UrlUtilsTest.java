package com.termux.shared.net.url;

import com.termux.shared.net.url.UrlUtils.UrlPart;

import org.junit.Assert;
import org.junit.Test;

public class UrlUtilsTest {

    private static final String FULL_URL = "https://user:pass@example.com:8443/path/to/file?q=1#frag";

    @Test
    public void joinUrlResolvesRelativeDestination() {
        Assert.assertEquals("https://example.com/a/c", UrlUtils.joinUrl("https://example.com/a/b", "c", false));
    }

    @Test
    public void joinUrlUsesAbsoluteDestinationWhenProvided() {
        Assert.assertEquals("https://other.com/x", UrlUtils.joinUrl("https://example.com/a/b", "https://other.com/x", false));
    }

    @Test
    public void joinUrlReturnsNullForNullOrEmptyBase() {
        Assert.assertNull(UrlUtils.joinUrl(null, "c", false));
        Assert.assertNull(UrlUtils.joinUrl("", "c", false));
    }

    @Test
    public void joinUrlReturnsNullForMalformedBase() {
        Assert.assertNull(UrlUtils.joinUrl("not a url", "c", false));
    }

    @Test
    public void getUrlReturnsNullForInvalidOrEmptyInput() {
        Assert.assertNull(UrlUtils.getUrl(null));
        Assert.assertNull(UrlUtils.getUrl(""));
        Assert.assertNull(UrlUtils.getUrl("missing-scheme.com"));
    }

    @Test
    public void getUrlParsesValidUrl() {
        Assert.assertNotNull(UrlUtils.getUrl("https://example.com"));
    }

    @Test
    public void getUrlPartExtractsEachRequestedComponent() {
        Assert.assertEquals("https", UrlUtils.getUrlPart(FULL_URL, UrlPart.PROTOCOL));
        Assert.assertEquals("example.com", UrlUtils.getUrlPart(FULL_URL, UrlPart.HOST));
        Assert.assertEquals("8443", UrlUtils.getUrlPart(FULL_URL, UrlPart.PORT));
        Assert.assertEquals("/path/to/file", UrlUtils.getUrlPart(FULL_URL, UrlPart.PATH));
        Assert.assertEquals("q=1", UrlUtils.getUrlPart(FULL_URL, UrlPart.QUERY));
        Assert.assertEquals("frag", UrlUtils.getUrlPart(FULL_URL, UrlPart.REF));
        Assert.assertEquals("frag", UrlUtils.getUrlPart(FULL_URL, UrlPart.FRAGMENT));
        Assert.assertEquals("user:pass", UrlUtils.getUrlPart(FULL_URL, UrlPart.USER_INFO));
        Assert.assertEquals("user:pass@example.com:8443", UrlUtils.getUrlPart(FULL_URL, UrlPart.AUTHORITY));
        Assert.assertEquals("/path/to/file?q=1", UrlUtils.getUrlPart(FULL_URL, UrlPart.FILE));
    }

    @Test
    public void getUrlPartReturnsNullForInvalidUrl() {
        Assert.assertNull(UrlUtils.getUrlPart("not-a-url", UrlPart.HOST));
    }

    @Test
    public void removeProtocolStripsSchemeAndWww() {
        Assert.assertEquals("example.com/x", UrlUtils.removeProtocol("https://www.example.com/x"));
        Assert.assertEquals("example.com/x", UrlUtils.removeProtocol("http://example.com/x"));
        Assert.assertEquals("example.com/x", UrlUtils.removeProtocol("www.example.com/x"));
        Assert.assertNull(UrlUtils.removeProtocol(null));
    }

    @Test
    public void areUrlsEqualIgnoresProtocolWwwAndTrailingSlashes() {
        Assert.assertTrue(UrlUtils.areUrlsEqual("https://www.example.com/path/", "http://example.com/path"));
        Assert.assertTrue(UrlUtils.areUrlsEqual(null, null));
        Assert.assertFalse(UrlUtils.areUrlsEqual(null, "https://example.com"));
        Assert.assertFalse(UrlUtils.areUrlsEqual("https://example.com/a", "https://example.com/b"));
    }
}
