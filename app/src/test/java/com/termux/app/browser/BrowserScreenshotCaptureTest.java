package com.termux.app.browser;

import android.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserScreenshotCaptureTest {

    private static byte[] allByteValues() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    @Test
    public void deliveryCommandEmbedsBase64DataInTheStream() {
        byte[] pngBytes = allByteValues();
        String expectedBase64 = Base64.encodeToString(pngBytes, Base64.DEFAULT).trim();
        String command = BrowserScreenshotCapture.buildDeliveryCommand(pngBytes, "browser-screenshot.png");
        Assert.assertTrue(command.contains(expectedBase64));
    }

    @Test
    public void deliveryCommandStreamsThroughHeredocSoDataTravelsOverThePty() {
        String command = BrowserScreenshotCapture.buildDeliveryCommand(allByteValues(), "browser-screenshot.png");
        Assert.assertTrue(command.contains("base64 -d > browser-screenshot.png <<'"));
    }

    @Test
    public void deliveryCommandDecodesToRelativePathInRemoteCurrentDirectory() {
        String command = BrowserScreenshotCapture.buildDeliveryCommand(allByteValues(), "browser-screenshot.png");
        Assert.assertTrue(command.contains("> browser-screenshot.png"));
        Assert.assertFalse(command.contains("/data/"));
        Assert.assertFalse(command.contains("/home/"));
        Assert.assertFalse(command.contains("> /"));
    }

    @Test
    public void deliveryCommandEndsWithNewlineSoTheRemoteShellExecutesIt() {
        String command = BrowserScreenshotCapture.buildDeliveryCommand(allByteValues(), "browser-screenshot.png");
        Assert.assertTrue(command.endsWith("\n"));
    }

    @Test
    public void deliveredBase64RoundTripsToOriginalPngBytes() {
        byte[] pngBytes = allByteValues();
        String command = BrowserScreenshotCapture.buildDeliveryCommand(pngBytes, "browser-screenshot.png");
        int bodyStart = command.indexOf('\n') + 1;
        int bodyEnd = command.lastIndexOf("TERMUX_BROWSER_SCREENSHOT_B64");
        String base64Body = command.substring(bodyStart, bodyEnd).trim();
        byte[] decoded = Base64.decode(base64Body, Base64.DEFAULT);
        Assert.assertArrayEquals(pngBytes, decoded);
    }

    @Test
    public void deliveryCommandQuotesHeredocDelimiterSoBase64IsNotShellExpanded() {
        String command = BrowserScreenshotCapture.buildDeliveryCommand(allByteValues(), "browser-screenshot.png");
        Assert.assertTrue(command.contains("<<'TERMUX_BROWSER_SCREENSHOT_B64'"));
    }

    @Test
    public void deliveryCommandDoesNotExecuteArbitraryScript() {
        String command = BrowserScreenshotCapture.buildDeliveryCommand(allByteValues(), "browser-screenshot.png");
        Assert.assertFalse(command.contains("eval"));
        Assert.assertFalse(command.contains("sh -c"));
        Assert.assertFalse(command.contains("curl"));
    }
}
