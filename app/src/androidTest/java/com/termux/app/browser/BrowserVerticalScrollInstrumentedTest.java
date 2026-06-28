package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
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
        loadTallPage(ref.get());

        assertEquals("page must start at the top", 0, scrollY(ref.get()));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();

        for (int i = 0; i < 5; i++) {
            device.swipe(width / 2, (int) (height * 0.72), width / 2, (int) (height * 0.25), 50);
            Thread.sleep(1000);
        }
        int afterScrollDown = scrollY(ref.get());
        assertTrue("vertical drag must scroll the tall page down (scrollY=" + afterScrollDown + ")",
            afterScrollDown > 200);

        Thread.sleep(2000);

        for (int i = 0; i < 8; i++) {
            device.swipe(width / 2, (int) (height * 0.25), width / 2, (int) (height * 0.72), 50);
            Thread.sleep(1000);
        }
        int afterScrollUp = scrollY(ref.get());
        assertTrue("vertical drag must scroll the tall page back to the top (scrollY=" + afterScrollUp + ")",
            afterScrollUp < 200);
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
        CountDownLatch latch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    latch.countDown();
                }
            });
            activity.webView.loadData(html, "text/html", "utf-8");
        });
        latch.await(10, TimeUnit.SECONDS);
        Thread.sleep(500);
    }

    private int scrollY(HostActivity activity) {
        AtomicReference<Integer> value = new AtomicReference<>(0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            value.set(activity.webView.getScrollY()));
        return value.get();
    }
}
