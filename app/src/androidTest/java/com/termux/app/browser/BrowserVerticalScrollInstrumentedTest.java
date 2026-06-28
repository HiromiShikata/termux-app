package com.termux.app.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserVerticalScrollInstrumentedTest {

    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 20;

    private static final int SCROLLABLE_READINESS_TIMEOUT_MILLIS = 10000;

    private static final int READINESS_POLL_INTERVAL_MILLIS = 100;

    private static final int MAX_GESTURE_ATTEMPTS = 8;

    private static final int GESTURE_SETTLE_MILLIS = 600;

    public static final class HostActivity extends Activity {
        public BrowserPinchAwareSwipeRefreshLayout swipeRefresh;
        public WebView webView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            FrameLayout root = new FrameLayout(this);
            swipeRefresh = new BrowserPinchAwareSwipeRefreshLayout(this);
            webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            BrowserWebViewConfigurator.apply(settings, BrowserViewMode.MOBILE, settings.getUserAgentString());
            swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                BrowserPullToRefreshGate.canWebViewScrollUp(webView));
            swipeRefresh.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(swipeRefresh, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(root);
        }
    }

    @Test
    public void tallPageScrollsDownAndBackToTopWithVerticalDragGestures() throws Exception {
        ActivityScenario<HostActivity> scenario = ActivityScenario.launch(HostActivity.class);
        AtomicReference<HostActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        HostActivity activity = ref.get();

        loadTallPage(activity);
        awaitScrollableFromTop(activity);

        assertFalse("page must start at the top", canScrollUp(activity));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();

        dragUntil(device, activity,
            width / 2, (int) (height * 0.72), width / 2, (int) (height * 0.25),
            () -> canScrollUp(activity));
        assertTrue("vertical drag must scroll the tall page down so it can scroll back up (scrollY="
                + scrollY(activity) + ")", canScrollUp(activity));

        dragUntil(device, activity,
            width / 2, (int) (height * 0.25), width / 2, (int) (height * 0.72),
            () -> !canScrollUp(activity));
        assertFalse("vertical drag must scroll the tall page back to the top (scrollY="
                + scrollY(activity) + ")", canScrollUp(activity));
    }

    private void dragUntil(UiDevice device, HostActivity activity,
            int startX, int startY, int endX, int endY, Condition condition) throws Exception {
        for (int attempt = 0; attempt < MAX_GESTURE_ATTEMPTS; attempt++) {
            if (condition.isMet()) {
                return;
            }
            device.swipe(startX, startY, endX, endY, 25);
            Thread.sleep(GESTURE_SETTLE_MILLIS);
        }
    }

    private interface Condition {
        boolean isMet();
    }

    private void loadTallPage(HostActivity activity) throws Exception {
        StringBuilder body = new StringBuilder();
        for (int section = 1; section <= 30; section++) {
            body.append("<div style='height:300px;background:")
                .append(section % 2 == 0 ? "#1565C0" : "#2E7D32")
                .append(";color:#fff;font-size:40px'>SECTION ").append(section).append("</div>");
        }
        String html = "<!doctype html><html><head><meta name='viewport' "
            + "content='width=device-width,initial-scale=1'></head><body style='margin:0'>"
            + body + "</body></html>";
        CountDownLatch pageFinished = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    pageFinished.countDown();
                }
            });
            activity.webView.loadData(html, "text/html", "utf-8");
        });
        assertTrue("tall page did not finish loading within timeout",
            pageFinished.await(PAGE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void awaitScrollableFromTop(HostActivity activity) throws Exception {
        long deadline = System.currentTimeMillis() + SCROLLABLE_READINESS_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (canScrollDown(activity)) {
                return;
            }
            Thread.sleep(READINESS_POLL_INTERVAL_MILLIS);
        }
        assertTrue("tall page never became vertically scrollable", canScrollDown(activity));
    }

    private boolean canScrollUp(HostActivity activity) {
        AtomicReference<Boolean> value = new AtomicReference<>(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            value.set(activity.webView.canScrollVertically(BrowserPullToRefreshGate.SCROLL_UP_DIRECTION)));
        return value.get();
    }

    private boolean canScrollDown(HostActivity activity) {
        AtomicReference<Boolean> value = new AtomicReference<>(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            value.set(activity.webView.canScrollVertically(1)));
        return value.get();
    }

    private int scrollY(HostActivity activity) {
        AtomicReference<Integer> value = new AtomicReference<>(0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            value.set(activity.webView.getScrollY()));
        return value.get();
    }
}
