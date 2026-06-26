package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ProjectBrowserMobileViewInstrumentedTest {

    private static final long CALLBACK_TIMEOUT_SECONDS = 10L;

    private static final long INNER_WIDTH_POLL_TIMEOUT_MILLIS = 10_000L;

    private static final long INNER_WIDTH_POLL_INTERVAL_MILLIS = 200L;

    private static final int WEB_VIEW_PHYSICAL_WIDTH_PX = 1080;

    private static final int WEB_VIEW_PHYSICAL_HEIGHT_PX = 1920;

    private static final int MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX = 900;

    private static final String PROJECT_URL = "https://github.com/HiromiShikata/termux-app";

    private static final String WARM_UP_PAGE_URL = "https://github.com/HiromiShikata";

    private static final String READ_INNER_WIDTH_SCRIPT = "window.innerWidth";

    private static final String READ_DEVICE_PIXEL_RATIO_SCRIPT = "window.devicePixelRatio";

    private static final String READ_DOCUMENT_CLIENT_WIDTH_SCRIPT =
        "document.documentElement.clientWidth";

    private static final double FULL_DEVICE_WIDTH_TOLERANCE_CSS_PX = 4.0;

    private static final String WARM_UP_PAGE_HTML =
        "<!DOCTYPE html><html><head><title>Warm up</title></head>"
            + "<body><h1>Warm up</h1></body></html>";

    private static final String MOBILE_VIEWPORT_PROJECT_PAGE_HTML =
        "<!DOCTYPE html><html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>Project overview</title></head>"
            + "<body><h1>Project overview</h1></body></html>";

    private static final String NO_VIEWPORT_FIXED_WIDTH_PROJECT_PAGE_HTML =
        "<!DOCTYPE html><html><head>"
            + "<title>Project overview</title>"
            + "<style>html,body{margin:0;padding:0;}"
            + "body{width:320px;}</style></head>"
            + "<body><h1>Project overview</h1></body></html>";

    @Test
    public void mobileConfigurationKeepsDefaultMobileUserAgent() {
        AtomicReference<WebSettings> settingsRef = new AtomicReference<>();
        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.MOBILE,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            settingsRef.set(webView.getSettings());
        });

        String userAgent = settingsRef.get().getUserAgentString();

        assertNotEquals(BrowserUserAgent.DESKTOP_USER_AGENT, userAgent);
        assertTrue("default WebView user agent should advertise Android: " + userAgent,
            userAgent.contains("Android"));
        assertTrue("default WebView user agent should advertise Mobile: " + userAgent,
            userAgent.contains("Mobile"));
    }

    @Test
    public void projectBrowserWebViewClientRendersMobileViewportPageAtMobileLayoutWidth()
        throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> pageFinishedLatchRef =
            new AtomicReference<>(new CountDownLatch(1));

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.MOBILE,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            webView.setWebViewClient(new BrowserMobileViewportWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    pageFinishedLatchRef.get().countDown();
                }
            });
            layoutOffscreen(webView);
            webViewRef.set(webView);
            webView.loadDataWithBaseURL(WARM_UP_PAGE_URL, WARM_UP_PAGE_HTML,
                "text/html", "utf-8", null);
        });

        assertTrue("warm up page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebView webView = webViewRef.get();
        pageFinishedLatchRef.set(new CountDownLatch(1));
        runOnMainSync(() -> webView.loadDataWithBaseURL(PROJECT_URL,
            MOBILE_VIEWPORT_PROJECT_PAGE_HTML, "text/html", "utf-8", null));

        assertTrue("project page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        int innerWidth = pollInnerWidthUntilMobileLayoutWidth(webView);

        assertNotEquals(
            "window.innerWidth was forced to the desktop layout width; the page rendered as desktop",
            BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX, innerWidth);
        assertTrue(
            "window.innerWidth (" + innerWidth + ") was not within a mobile device width",
            innerWidth > 0 && innerWidth <= MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX);
    }

    @Test
    public void mobileConfigurationRendersDeviceWidthViewportAtFullDeviceWidth()
        throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> pageFinishedLatchRef =
            new AtomicReference<>(new CountDownLatch(1));

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.MOBILE,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            webView.setWebViewClient(new BrowserMobileViewportWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    pageFinishedLatchRef.get().countDown();
                }
            });
            layoutOffscreen(webView);
            webViewRef.set(webView);
            webView.loadDataWithBaseURL(WARM_UP_PAGE_URL, WARM_UP_PAGE_HTML,
                "text/html", "utf-8", null);
        });

        assertTrue("warm up page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebView webView = webViewRef.get();
        pageFinishedLatchRef.set(new CountDownLatch(1));
        runOnMainSync(() -> webView.loadDataWithBaseURL(PROJECT_URL,
            MOBILE_VIEWPORT_PROJECT_PAGE_HTML, "text/html", "utf-8", null));

        assertTrue("project page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        int innerWidth = pollInnerWidthUntilMobileLayoutWidth(webView);
        double devicePixelRatio = readDouble(webView, READ_DEVICE_PIXEL_RATIO_SCRIPT);
        int documentClientWidth = readInt(webView, READ_DOCUMENT_CLIENT_WIDTH_SCRIPT);
        double expectedDeviceWidthCssPx = WEB_VIEW_PHYSICAL_WIDTH_PX / devicePixelRatio;

        assertEquals(
            "window.innerWidth (" + innerWidth + ") did not match the document layout width ("
                + documentClientWidth + "); the page rendered in a narrow column",
            documentClientWidth, innerWidth);
        assertEquals(
            "window.innerWidth (" + innerWidth + ") did not fill the device width ("
                + expectedDeviceWidthCssPx + " CSS px); the page rendered in a narrow column",
            expectedDeviceWidthCssPx, innerWidth, FULL_DEVICE_WIDTH_TOLERANCE_CSS_PX);
    }

    @Test
    public void mobileConfigurationForcesNoViewportFixedWidthPageToFullDeviceWidth()
        throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> pageFinishedLatchRef =
            new AtomicReference<>(new CountDownLatch(1));

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.MOBILE,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            webView.setWebViewClient(new BrowserMobileViewportWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    pageFinishedLatchRef.get().countDown();
                }
            });
            layoutOffscreen(webView);
            webViewRef.set(webView);
            webView.loadDataWithBaseURL(WARM_UP_PAGE_URL, WARM_UP_PAGE_HTML,
                "text/html", "utf-8", null);
        });

        assertTrue("warm up page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebView webView = webViewRef.get();
        pageFinishedLatchRef.set(new CountDownLatch(1));
        runOnMainSync(() -> webView.loadDataWithBaseURL(PROJECT_URL,
            NO_VIEWPORT_FIXED_WIDTH_PROJECT_PAGE_HTML, "text/html", "utf-8", null));

        assertTrue("project page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        int innerWidth = pollInnerWidthUntilMobileLayoutWidth(webView);
        double devicePixelRatio = readDouble(webView, READ_DEVICE_PIXEL_RATIO_SCRIPT);
        int documentClientWidth = readInt(webView, READ_DOCUMENT_CLIENT_WIDTH_SCRIPT);
        double expectedDeviceWidthCssPx = WEB_VIEW_PHYSICAL_WIDTH_PX / devicePixelRatio;

        assertEquals(
            "window.innerWidth (" + innerWidth + ") did not match the document layout width ("
                + documentClientWidth + "); the no-viewport page was not forced to device width",
            documentClientWidth, innerWidth);
        assertEquals(
            "window.innerWidth (" + innerWidth + ") did not fill the device width ("
                + expectedDeviceWidthCssPx + " CSS px); the no-viewport page rendered in a "
                + "fallback wide column instead of the forced device width",
            expectedDeviceWidthCssPx, innerWidth, FULL_DEVICE_WIDTH_TOLERANCE_CSS_PX);
    }

    private int pollInnerWidthUntilMobileLayoutWidth(WebView webView) throws InterruptedException {
        int innerWidth = -1;
        long deadline = System.currentTimeMillis() + INNER_WIDTH_POLL_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            innerWidth = readInnerWidth(webView);
            if (innerWidth > 0 && innerWidth <= MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX) {
                return innerWidth;
            }
            Thread.sleep(INNER_WIDTH_POLL_INTERVAL_MILLIS);
        }
        return innerWidth;
    }

    private int readInnerWidth(WebView webView) throws InterruptedException {
        return readInt(webView, READ_INNER_WIDTH_SCRIPT);
    }

    private int readInt(WebView webView, String script) throws InterruptedException {
        String value = stripJsonStringQuotes(evaluateJavascript(webView, script));
        if (value == null) {
            return -1;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private double readDouble(WebView webView, String script) throws InterruptedException {
        String value = stripJsonStringQuotes(evaluateJavascript(webView, script));
        if (value == null) {
            return -1;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private static void layoutOffscreen(WebView webView) {
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(WEB_VIEW_PHYSICAL_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(WEB_VIEW_PHYSICAL_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        webView.layout(0, 0, WEB_VIEW_PHYSICAL_WIDTH_PX, WEB_VIEW_PHYSICAL_HEIGHT_PX);
    }

    private String evaluateJavascript(WebView webView, String script) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resultRef = new AtomicReference<>();
        runOnMainSync(() -> webView.evaluateJavascript(script, value -> {
            resultRef.set(value);
            latch.countDown();
        }));
        assertTrue("javascript evaluation timed out",
            latch.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return resultRef.get();
    }

    private static String stripJsonStringQuotes(String jsonStringValue) {
        if (jsonStringValue != null
            && jsonStringValue.length() >= 2
            && jsonStringValue.startsWith("\"")
            && jsonStringValue.endsWith("\"")) {
            return jsonStringValue.substring(1, jsonStringValue.length() - 1);
        }
        return jsonStringValue;
    }

    private static void runOnMainSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
