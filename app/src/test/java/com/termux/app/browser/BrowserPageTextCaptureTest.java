package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserPageTextCaptureTest {

    @Test
    public void captureScriptReadsBodyInnerText() {
        Assert.assertTrue(BrowserPageTextCapture.CAPTURE_SCRIPT.contains("document.body"));
        Assert.assertTrue(BrowserPageTextCapture.CAPTURE_SCRIPT.contains("innerText"));
    }

    @Test
    public void captureScriptFallsBackToDocumentElementAndTextContent() {
        Assert.assertTrue(BrowserPageTextCapture.CAPTURE_SCRIPT.contains("document.documentElement"));
        Assert.assertTrue(BrowserPageTextCapture.CAPTURE_SCRIPT.contains("textContent"));
    }

    @Test
    public void parseCapturedTextDecodesNewlines() throws Exception {
        Assert.assertEquals("Hello\nworld",
            BrowserPageTextCapture.parseCapturedText("\"Hello\\nworld\""));
    }

    @Test
    public void parseCapturedTextDecodesEscapedQuotes() throws Exception {
        Assert.assertEquals("say \"hi\"",
            BrowserPageTextCapture.parseCapturedText("\"say \\\"hi\\\"\""));
    }

    @Test
    public void parseCapturedTextDecodesUnicodeEscape() throws Exception {
        Assert.assertEquals("café",
            BrowserPageTextCapture.parseCapturedText("\"caf\\u00e9\""));
    }

    @Test
    public void parseCapturedTextReturnsEmptyForNullAndBlankAndNullLiteral() throws Exception {
        Assert.assertEquals("", BrowserPageTextCapture.parseCapturedText(null));
        Assert.assertEquals("", BrowserPageTextCapture.parseCapturedText(""));
        Assert.assertEquals("", BrowserPageTextCapture.parseCapturedText("   "));
        Assert.assertEquals("", BrowserPageTextCapture.parseCapturedText("null"));
    }

    @Test
    public void parseCapturedTextReturnsEmptyForEmptyJsonString() throws Exception {
        Assert.assertEquals("", BrowserPageTextCapture.parseCapturedText("\"\""));
    }
}
