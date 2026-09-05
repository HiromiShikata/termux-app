package com.termux.app.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.termux.app.RetryRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserInnerScrollPullToRefreshInstrumentedTest {

    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 20;
    private static final int INNER_SCROLL_READINESS_TIMEOUT_MILLIS = 5000;
    private static final int READINESS_POLL_INTERVAL_MILLIS = 100;
    private static final int GESTURE_SETTLE_MILLIS = 800;
    private static final int GESTURE_SWIPE_STEPS = 20;
    private static final int TEST_RETRY_ATTEMPTS = 3;

    @Rule
    public final RetryRule retryRule = new RetryRule(TEST_RETRY_ATTEMPTS);

    public static final class HostActivity extends Activity {
        public BrowserPinchAwareSwipeRefreshLayout swipeRefresh;
        public WebView webView;
        public BrowserWebViewScrollTracker scrollTracker;
        public final AtomicBoolean refreshTriggered = new AtomicBoolean(false);

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            FrameLayout root = new FrameLayout(this);
            swipeRefresh = new BrowserPinchAwareSwipeRefreshLayout(this);
            webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            scrollTracker = new BrowserWebViewScrollTracker();
            scrollTracker.attach(webView);
            webView.addJavascriptInterface(
                new BrowserInnerScrollJavascriptInterface(webView, scrollTracker),
                BrowserInnerScrollJavascriptInterface.INTERFACE_NAME);
            swipeRefresh.setEnabled(true);
            swipeRefresh.setOnRefreshListener(() -> {
                refreshTriggered.set(true);
                swipeRefresh.setRefreshing(false);
            });
            swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                !scrollTracker.isAtTop(webView));
            swipeRefresh.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(swipeRefresh, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(root);
        }
    }

    @Test
    public void upwardDragOnScrolledInnerElementDoesNotTriggerPullToRefresh() throws Exception {
        ActivityScenario<HostActivity> scenario = ActivityScenario.launch(HostActivity.class);
        AtomicReference<HostActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        HostActivity activity = ref.get();

        loadFullViewportPageWithScrollableInner(activity);
        scrollInnerElementDown(activity);
        awaitInnerScrollDetected(activity);

        assertFalse("inner-scroll tracker must report not-at-top after inner element scrolled",
            isAtTop(activity));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        int centerX = width / 2;
        int highY = (int) (height * 0.2);
        int lowY = (int) (height * 0.8);

        device.swipe(centerX, highY, centerX, lowY, GESTURE_SWIPE_STEPS);
        Thread.sleep(GESTURE_SETTLE_MILLIS);

        assertFalse("upward drag on a scrolled inner element must not trigger pull-to-refresh",
            activity.refreshTriggered.get());
    }

    private void loadFullViewportPageWithScrollableInner(HostActivity activity) throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            items.append("<div style='height:60px;background:")
                .append(i % 2 == 0 ? "#1565C0" : "#2E7D32")
                .append(";color:#fff'>ITEM ").append(i).append("</div>");
        }
        String html = "<!doctype html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "html,body{margin:0;height:100%;}"
            + ".outer{height:100%;display:flex;flex-direction:column;overflow:hidden;}"
            + ".inner{flex:1;min-height:0;overflow-y:auto;}"
            + "</style></head><body>"
            + "<div class='outer'><div class='inner' id='list'>" + items + "</div></div>"
            + "</body></html>";
        CountDownLatch pageFinished = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    activity.scrollTracker.resetToTop(view);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    view.evaluateJavascript(BrowserInnerScrollJavascriptInterface.INJECTION_SCRIPT, null);
                    pageFinished.countDown();
                }
            });
            activity.webView.loadData(html, "text/html", "utf-8");
        });
        assertTrue("full-viewport page did not finish loading within timeout",
            pageFinished.await(PAGE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void scrollInnerElementDown(HostActivity activity) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            activity.webView.evaluateJavascript(
                "document.getElementById('list').scrollTop = 300;",
                value -> done.countDown()));
        done.await(5, TimeUnit.SECONDS);
    }

    private void awaitInnerScrollDetected(HostActivity activity) throws InterruptedException {
        long deadline = System.currentTimeMillis() + INNER_SCROLL_READINESS_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (!isAtTop(activity)) {
                return;
            }
            Thread.sleep(READINESS_POLL_INTERVAL_MILLIS);
        }
        assertFalse("inner scroll was never detected by the JS bridge within timeout",
            isAtTop(activity));
    }

    private boolean isAtTop(HostActivity activity) {
        AtomicReference<Boolean> value = new AtomicReference<>(true);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            value.set(activity.scrollTracker.isAtTop(activity.webView)));
        return value.get();
    }
}
