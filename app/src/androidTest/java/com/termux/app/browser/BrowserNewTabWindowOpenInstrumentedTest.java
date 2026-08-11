package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserNewTabWindowOpenInstrumentedTest {

    private static final long CALLBACK_TIMEOUT_SECONDS = 15L;

    private static final int WEB_VIEW_PHYSICAL_WIDTH_PX = 1080;

    private static final int WEB_VIEW_PHYSICAL_HEIGHT_PX = 1920;

    private static final String OPENER_BASE_URL = "https://example.com/opener";

    private static final String NEW_TAB_URL = "https://example.com/new-tab";

    private static final String WINDOW_OPEN_PAGE_HTML =
        "<!DOCTYPE html><html><head><title>Opener</title></head>"
            + "<body><script>window.open('" + NEW_TAB_URL + "', '_blank');</script></body></html>";

    @Test
    public void windowOpenReachesOnCreateWindowAndYieldsTheNewTabUrlForTheInAppBrowser()
            throws InterruptedException {
        AtomicReference<WebView> openerWebViewRef = new AtomicReference<>();
        AtomicReference<String> newTabUrlRef = new AtomicReference<>();
        CountDownLatch newWindowLatch = new CountDownLatch(1);

        runOnMainSync(() -> {
            WebView openerWebView = new WebView(targetContext());
            BrowserWebViewConfigurator.apply(openerWebView, BrowserViewMode.DESKTOP,
                openerWebView.getSettings().getUserAgentString());
            openerWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                              Message resultMsg) {
                    WebView newWindowUrlProbeWebView = new WebView(view.getContext());
                    newWindowUrlProbeWebView.setWebViewClient(new BrowserNewWindowUrlProbeWebViewClient(
                        url -> {
                            newTabUrlRef.set(url);
                            newWindowLatch.countDown();
                            return true;
                        }));
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(newWindowUrlProbeWebView);
                    resultMsg.sendToTarget();
                    return true;
                }
            });
            layoutOffscreen(openerWebView);
            openerWebViewRef.set(openerWebView);
            openerWebView.loadDataWithBaseURL(OPENER_BASE_URL, WINDOW_OPEN_PAGE_HTML,
                "text/html", "utf-8", null);
        });

        assertTrue("window.open() did not reach onCreateWindow within timeout",
            newWindowLatch.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(NEW_TAB_URL, newTabUrlRef.get());

        runOnMainSync(() -> openerWebViewRef.get().destroy());
    }

    private static void layoutOffscreen(WebView webView) {
        webView.setLayoutParams(new ViewGroup.LayoutParams(
            WEB_VIEW_PHYSICAL_WIDTH_PX, WEB_VIEW_PHYSICAL_HEIGHT_PX));
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(WEB_VIEW_PHYSICAL_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(WEB_VIEW_PHYSICAL_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        webView.layout(0, 0, WEB_VIEW_PHYSICAL_WIDTH_PX, WEB_VIEW_PHYSICAL_HEIGHT_PX);
    }

    private static void runOnMainSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
