package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserCoreWebViewClientInstrumentedTest {

    private static final long CALLBACK_TIMEOUT_SECONDS = 10L;

    private static final long INNER_WIDTH_POLL_TIMEOUT_MILLIS = 10_000L;

    private static final long INNER_WIDTH_POLL_INTERVAL_MILLIS = 200L;

    private static final int WEB_VIEW_PHYSICAL_WIDTH_PX = 1080;

    private static final int WEB_VIEW_PHYSICAL_HEIGHT_PX = 1920;

    private static final int MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX = 900;

    private static final double DESKTOP_LAYOUT_WIDTH_TOLERANCE_CSS_PX = 4.0;

    private static final String PROJECT_URL = "https://github.com/HiromiShikata/termux-app";

    private static final String WARM_UP_PAGE_URL = "https://github.com/HiromiShikata";

    private static final String READ_INNER_WIDTH_SCRIPT = "window.innerWidth";

    private static final String WARM_UP_PAGE_HTML =
        "<!DOCTYPE html><html><head><title>Warm up</title></head>"
            + "<body><h1>Warm up</h1></body></html>";

    private static final String MOBILE_VIEWPORT_PAGE_HTML =
        "<!DOCTYPE html><html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>Project overview</title></head>"
            + "<body><h1>Project overview</h1></body></html>";

    private static final class StubHost implements BrowserCoreWebViewClient.Host {

        private final BrowserViewMode viewMode;
        private final boolean injectMobileViewport;
        private final CountDownLatch pageFinishedLatch;

        StubHost(BrowserViewMode viewMode, boolean injectMobileViewport,
                 CountDownLatch pageFinishedLatch) {
            this.viewMode = viewMode;
            this.injectMobileViewport = injectMobileViewport;
            this.pageFinishedLatch = pageFinishedLatch;
        }

        @NonNull
        @Override
        public BrowserViewMode getViewMode() {
            return viewMode;
        }

        @Override
        public boolean shouldInjectMobileViewport() {
            return injectMobileViewport;
        }

        @Override
        public void onPageStarted(@NonNull WebView view, @Nullable String url) {
        }

        @Override
        public void onPageCommitVisible(@NonNull WebView view, @Nullable String url) {
        }

        @Override
        public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
            pageFinishedLatch.countDown();
            return false;
        }

        @Override
        public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
        }

        @Override
        public void onMainFrameError(@NonNull WebView view) {
        }

        @Override
        public boolean onRenderProcessGone(@NonNull WebView view, boolean didCrash) {
            return false;
        }

        @Override
        public void openInExternalBrowser(@NonNull String url) {
        }

        @Override
        public boolean openInMatchingNativeApp(@NonNull String url) {
            return false;
        }
    }

    @Test
    public void sharedClientRendersMobileModePageAtMobileLayoutWidth() throws InterruptedException {
        WebView webView = loadProjectPageWith(BrowserViewMode.MOBILE, true, MOBILE_VIEWPORT_PAGE_HTML);
        int innerWidth = pollInnerWidthUntilMobileLayoutWidth(webView);

        assertNotEquals(
            "shared client forced mobile-mode page to the desktop layout width",
            BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX, innerWidth);
        assertTrue(
            "window.innerWidth (" + innerWidth + ") was not within a mobile device width",
            innerWidth > 0 && innerWidth <= MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX);
    }

    @Test
    public void sharedClientRendersDesktopModePageAtDesktopLayoutWidth() throws InterruptedException {
        WebView webView = loadProjectPageWith(BrowserViewMode.DESKTOP, false, MOBILE_VIEWPORT_PAGE_HTML);
        int innerWidth = pollInnerWidthUntilDesktopLayoutWidth(webView);

        assertEquals(
            "shared client did not force desktop-mode page to the desktop layout width",
            BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX, innerWidth, DESKTOP_LAYOUT_WIDTH_TOLERANCE_CSS_PX);
    }

    private WebView loadProjectPageWith(BrowserViewMode viewMode, boolean injectMobileViewport,
                                        String projectPageHtml) throws InterruptedException {
        AtomicReference<WebView> webViewRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> pageFinishedLatchRef =
            new AtomicReference<>(new CountDownLatch(1));

        runOnMainSync(() -> {
            WebView webView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(webView, viewMode,
                webView.getSettings().getUserAgentString());
            webView.setWebViewClient(new BrowserCoreWebViewClient(
                new StubHost(viewMode, injectMobileViewport, pageFinishedLatchRef.get())));
            layoutOffscreen(webView);
            webViewRef.set(webView);
            webView.loadDataWithBaseURL(WARM_UP_PAGE_URL, WARM_UP_PAGE_HTML, "text/html", "utf-8", null);
        });

        assertTrue("warm up page did not finish loading within timeout",
            pageFinishedLatchRef.get().await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebView webView = webViewRef.get();
        CountDownLatch projectLatch = new CountDownLatch(1);
        runOnMainSync(() -> {
            webView.setWebViewClient(new BrowserCoreWebViewClient(
                new StubHost(viewMode, injectMobileViewport, projectLatch)));
            webView.loadDataWithBaseURL(PROJECT_URL, projectPageHtml, "text/html", "utf-8", null);
        });

        assertTrue("project page did not finish loading within timeout",
            projectLatch.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return webView;
    }

    private int pollInnerWidthUntilMobileLayoutWidth(WebView webView) throws InterruptedException {
        long deadline = System.currentTimeMillis() + INNER_WIDTH_POLL_TIMEOUT_MILLIS;
        int innerWidth = readInt(webView, READ_INNER_WIDTH_SCRIPT);
        while (innerWidth > MOBILE_DEVICE_WIDTH_UPPER_BOUND_CSS_PX
            && System.currentTimeMillis() < deadline) {
            Thread.sleep(INNER_WIDTH_POLL_INTERVAL_MILLIS);
            innerWidth = readInt(webView, READ_INNER_WIDTH_SCRIPT);
        }
        return innerWidth;
    }

    private int pollInnerWidthUntilDesktopLayoutWidth(WebView webView) throws InterruptedException {
        long deadline = System.currentTimeMillis() + INNER_WIDTH_POLL_TIMEOUT_MILLIS;
        int innerWidth = readInt(webView, READ_INNER_WIDTH_SCRIPT);
        while (Math.abs(innerWidth - BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX)
                > DESKTOP_LAYOUT_WIDTH_TOLERANCE_CSS_PX
            && System.currentTimeMillis() < deadline) {
            Thread.sleep(INNER_WIDTH_POLL_INTERVAL_MILLIS);
            innerWidth = readInt(webView, READ_INNER_WIDTH_SCRIPT);
        }
        return innerWidth;
    }

    private int readInt(WebView webView, String script) throws InterruptedException {
        String value = evaluateJavascript(webView, script);
        try {
            return (int) Math.round(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void layoutOffscreen(WebView webView) {
        webView.setLayoutParams(new ViewGroup.LayoutParams(
            WEB_VIEW_PHYSICAL_WIDTH_PX, WEB_VIEW_PHYSICAL_HEIGHT_PX));
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
        assertTrue("evaluateJavascript did not return within timeout",
            latch.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return stripJsonStringQuotes(resultRef.get());
    }

    private static String stripJsonStringQuotes(String jsonStringValue) {
        if (jsonStringValue == null) {
            return "";
        }
        if (jsonStringValue.length() >= 2
            && jsonStringValue.startsWith("\"") && jsonStringValue.endsWith("\"")) {
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
