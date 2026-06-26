package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class ProjectBrowserDesktopViewInstrumentedTest {

    private static final long CALLBACK_TIMEOUT_SECONDS = 10L;

    private static final long INNER_WIDTH_POLL_TIMEOUT_MILLIS = 10_000L;

    private static final long INNER_WIDTH_POLL_INTERVAL_MILLIS = 200L;

    private static final int WEB_VIEW_PHYSICAL_WIDTH_PX = 1080;

    private static final int WEB_VIEW_PHYSICAL_HEIGHT_PX = 1920;

    private static final int MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX = 900;

    private static final String PROJECT_URL = "https://github.com/HiromiShikata/termux-app";

    private static final String WARM_UP_PAGE_URL = "https://github.com/HiromiShikata";

    private static final String READ_INNER_WIDTH_SCRIPT = "window.innerWidth";

    private static final String WARM_UP_PAGE_HTML =
        "<!DOCTYPE html><html><head><title>Warm up</title></head>"
            + "<body><h1>Warm up</h1></body></html>";

    private static final String MOBILE_VIEWPORT_PROJECT_PAGE_HTML =
        "<!DOCTYPE html><html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>Project overview</title></head>"
            + "<body><h1>Project overview</h1></body></html>";

    private static final String READ_VIEWPORT_CONTENT_SCRIPT =
        "document.querySelector('meta[name=\"viewport\"]').getAttribute('content')";

    @Test
    public void desktopConfigurationProducesRealDesktopWebSettings() {
        AtomicReference<WebSettings> settingsRef = new AtomicReference<>();
        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.DESKTOP,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            settingsRef.set(webView.getSettings());
        });

        WebSettings settings = settingsRef.get();
        String userAgent = settings.getUserAgentString();

        assertEquals(BrowserUserAgent.DESKTOP_USER_AGENT, userAgent);
        assertFalse(userAgent.contains("Mobile"));
        assertFalse(userAgent.contains("wv"));
        assertTrue(settings.getUseWideViewPort());
        assertTrue(settings.getLoadWithOverviewMode());
    }

    @Test
    public void desktopViewportInjectionOverridesMobileViewportOnProjectUrlPage() throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        CountDownLatch pageFinished = new CountDownLatch(1);

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.DESKTOP,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    pageFinished.countDown();
                }
            });
            webViewRef.set(webView);
            webView.loadDataWithBaseURL(PROJECT_URL, MOBILE_VIEWPORT_PROJECT_PAGE_HTML,
                "text/html", "utf-8", null);
        });

        assertTrue("project page did not finish loading within timeout",
            pageFinished.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebView webView = webViewRef.get();
        String mobileViewport = stripJsonStringQuotes(
            evaluateJavascript(webView, READ_VIEWPORT_CONTENT_SCRIPT));
        assertEquals("width=device-width, initial-scale=1.0", mobileViewport);

        evaluateJavascript(webView, BrowserDesktopViewport.INJECTION_SCRIPT);

        String desktopViewport = stripJsonStringQuotes(
            evaluateJavascript(webView, READ_VIEWPORT_CONTENT_SCRIPT));
        assertEquals("width=" + BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX, desktopViewport);
    }

    @Test
    public void projectBrowserWebViewClientRendersMobileViewportPageAtDesktopLayoutWidth()
        throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> pageFinishedLatchRef =
            new AtomicReference<>(new CountDownLatch(1));

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView.getSettings(), BrowserViewMode.DESKTOP,
                BrowserUserAgent.normalizeDefault(webView.getSettings().getUserAgentString()));
            webView.setWebViewClient(new BrowserDesktopViewportWebViewClient() {
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

        int innerWidth = pollInnerWidthUntilDesktopLayoutWidth(webView);

        assertEquals(
            "window.innerWidth never reached the forced desktop layout width; last observed="
                + innerWidth,
            BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX, innerWidth);
        assertTrue(
            "window.innerWidth (" + innerWidth + ") was not clearly wider than a mobile device width",
            innerWidth > MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX);
    }

    private int pollInnerWidthUntilDesktopLayoutWidth(WebView webView) throws InterruptedException {
        int innerWidth = -1;
        long deadline = System.currentTimeMillis() + INNER_WIDTH_POLL_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            innerWidth = readInnerWidth(webView);
            if (innerWidth == BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX) {
                return innerWidth;
            }
            Thread.sleep(INNER_WIDTH_POLL_INTERVAL_MILLIS);
        }
        return innerWidth;
    }

    private int readInnerWidth(WebView webView) throws InterruptedException {
        String value = stripJsonStringQuotes(evaluateJavascript(webView, READ_INNER_WIDTH_SCRIPT));
        if (value == null) {
            return -1;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.trim()));
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
