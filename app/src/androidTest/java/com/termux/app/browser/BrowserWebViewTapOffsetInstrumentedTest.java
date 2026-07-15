package com.termux.app.browser;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserWebViewTapOffsetInstrumentedTest {

    private static final String LOG_TAG = "TapOffsetTest";

    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 20;

    private static final int READBACK_TIMEOUT_SECONDS = 10;

    private static final int LAYOUT_READINESS_TIMEOUT_MILLIS = 10_000;

    private static final int READINESS_POLL_INTERVAL_MILLIS = 100;

    private static final int TAP_SETTLE_MILLIS = 700;

    private static final int VIEWPORT_SETTLE_MILLIS = 1_500;

    private static final int TEST_RETRY_ATTEMPTS = 3;

    private static final int VISUAL_TOLERANCE_SCREEN_PX = 24;

    private static final String CAPTURE_PAGE_HTML =
        "<!doctype html><html><head>%s"
            + "<style>html,body{margin:0;padding:0;width:100%%;height:100%%}"
            + "#grid{position:fixed;inset:0;display:flex}"
            + ".cell{flex:1 1 0;height:100%%}"
            + "#left{background:#2E7D32}#mid{background:#1565C0}#right{background:#C62828}"
            + "</style></head><body>"
            + "<div id='grid'>"
            + "<div class='cell' id='left'></div>"
            + "<div class='cell' id='mid'></div>"
            + "<div class='cell' id='right'></div>"
            + "</div>"
            + "<script>"
            + "window.__tapClientX=-1;window.__tapClientY=-1;"
            + "window.__tapTargetId='';window.__tapCount=0;"
            + "document.addEventListener('click',function(e){"
            + "window.__tapClientX=e.clientX;window.__tapClientY=e.clientY;"
            + "var t=e.target;window.__tapTargetId=t?t.id:'';window.__tapCount++;},true);"
            + "</script></body></html>";

    private static final String MOBILE_VIEWPORT_META =
        "<meta name='viewport' content='width=device-width,initial-scale=1'>";

    private static final String OWN_WIDE_VIEWPORT_META =
        "<meta name='viewport' content='width=3000,initial-scale=1'>";

    @Rule
    public final RetryRule retryRule = new RetryRule(TEST_RETRY_ATTEMPTS);

    public static final class HostActivity extends Activity {
        public static volatile boolean sSplitLayout;
        public static volatile int sInjectedViewport;

        public static final int INJECT_NONE = 0;
        public static final int INJECT_MOBILE = 1;
        public static final int INJECT_DESKTOP = 2;

        public WebView webView;
        public BrowserPinchAwareSwipeRefreshLayout swipeRefresh;
        public FrameLayout browserColumn;
        public View terminalColumn;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            boolean splitLayout = sSplitLayout;
            LinearLayout splitRow = new LinearLayout(this);
            splitRow.setOrientation(LinearLayout.HORIZONTAL);

            browserColumn = new FrameLayout(this);
            swipeRefresh = new BrowserPinchAwareSwipeRefreshLayout(this);
            webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            BrowserViewMode viewMode = sInjectedViewport == INJECT_DESKTOP
                ? BrowserViewMode.DESKTOP : BrowserViewMode.MOBILE;
            BrowserWebViewConfigurator.apply(webView, viewMode, settings.getUserAgentString());
            swipeRefresh.setEnabled(true);
            swipeRefresh.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            browserColumn.addView(swipeRefresh, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            terminalColumn = new View(this);

            float browserWeight = splitLayout ? 0.5f : 1f;
            float terminalWeight = splitLayout ? 0.5f : 0f;
            LinearLayout.LayoutParams browserParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, browserWeight);
            LinearLayout.LayoutParams terminalParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, terminalWeight);
            splitRow.addView(browserColumn, browserParams);
            splitRow.addView(terminalColumn, terminalParams);
            setContentView(splitRow);
        }
    }

    @Test
    public void mobileInjectedViewportKeepsVisualAndHitAlignedInSplit() throws Exception {
        runVisualHitAlignment(true, HostActivity.INJECT_MOBILE, OWN_WIDE_VIEWPORT_META,
            "mobile-injected-split");
    }

    @Test
    public void desktopInjectedViewportKeepsVisualAndHitAlignedInSplit() throws Exception {
        runVisualHitAlignment(true, HostActivity.INJECT_DESKTOP, "", "desktop-injected-split");
    }

    @Test
    public void plainMobileViewportKeepsVisualAndHitAlignedInSplit() throws Exception {
        runVisualHitAlignment(true, HostActivity.INJECT_NONE, MOBILE_VIEWPORT_META,
            "plain-mobile-split");
    }

    private void runVisualHitAlignment(boolean splitLayout, int injectedViewport,
            String pageViewportMeta, String layoutLabel) throws Exception {
        HostActivity.sSplitLayout = splitLayout;
        HostActivity.sInjectedViewport = injectedViewport;
        ActivityScenario<HostActivity> scenario = ActivityScenario.launch(HostActivity.class);
        AtomicReference<HostActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        HostActivity activity = ref.get();

        loadCapturePage(activity, pageViewportMeta);
        applyInjectedViewport(activity, injectedViewport);
        awaitNonZeroWebViewWidth(activity);
        Thread.sleep(VIEWPORT_SETTLE_MILLIS);

        assertVisualTapHitsTarget(activity, layoutLabel);
    }

    private void assertVisualTapHitsTarget(HostActivity activity, String layoutLabel)
            throws Exception {
        int[] location = new int[2];
        int[] size = new int[2];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.getLocationOnScreen(location);
            size[0] = activity.webView.getWidth();
            size[1] = activity.webView.getHeight();
        });
        int webViewLeftPx = location[0];
        int webViewTopPx = location[1];
        int webViewWidthPx = size[0];
        int webViewHeightPx = size[1];
        assertTrue("web view must have a positive width", webViewWidthPx > 0);

        double density = activity.getResources().getDisplayMetrics().density;
        double scale = readNumber(activity, "visualViewportScale()");
        double innerWidth = readNumber(activity, "window.innerWidth");
        int tapScreenY = webViewTopPx + webViewHeightPx / 2;
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        String[] expectedByThird = {"left", "mid", "right"};
        int[] fractionNumerators = {1, 3, 5};
        StringBuilder observed = new StringBuilder();
        String firstMismatch = null;
        for (int third = 0; third < expectedByThird.length; third++) {
            resetTapRecord(activity);
            int tapScreenX = webViewLeftPx + webViewWidthPx * fractionNumerators[third] / 6;
            device.click(tapScreenX, tapScreenY);
            Thread.sleep(TAP_SETTLE_MILLIS);
            awaitTapRecorded(activity);
            String hitId = readString(activity, "window.__tapTargetId");
            observed.append(expectedByThird[third]).append("->").append(hitId).append(' ');
            if (firstMismatch == null && !expectedByThird[third].equals(hitId)) {
                firstMismatch = "tap at screen x=" + tapScreenX + " (visual "
                    + expectedByThird[third] + " third) activated '" + hitId + "'";
            }
        }

        Log.i(LOG_TAG, "layout=" + layoutLabel
            + " webViewLeftPx=" + webViewLeftPx
            + " webViewWidthPx=" + webViewWidthPx
            + " density=" + density
            + " innerWidthCss=" + innerWidth
            + " visualScale=" + scale
            + " observedThirds=[" + observed.toString().trim() + "]");

        assertTrue(
            "a tap on the visual thirds of the split browser column must activate the element"
                + " under the touch point (layout=" + layoutLabel + ", visualScale=" + scale
                + ", innerWidthCss=" + innerWidth + ", webViewWidthPx=" + webViewWidthPx
                + ", observed=" + observed.toString().trim() + "); " + firstMismatch,
            firstMismatch == null);
    }

    private void applyInjectedViewport(HostActivity activity, int injectedViewport) throws Exception {
        if (injectedViewport == HostActivity.INJECT_MOBILE) {
            evaluate(activity, BrowserMobileViewport.INJECTION_SCRIPT);
        } else if (injectedViewport == HostActivity.INJECT_DESKTOP) {
            evaluate(activity, BrowserDesktopViewport.INJECTION_SCRIPT);
        }
    }

    private void loadCapturePage(HostActivity activity, String pageViewportMeta) throws Exception {
        String html = String.format(CAPTURE_PAGE_HTML, pageViewportMeta);
        String withHelper = html.replace("</script></body>",
            "window.visualViewportScale=function(){return window.visualViewport"
                + "?window.visualViewport.scale:1;};</script></body>");
        CountDownLatch pageFinished = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    pageFinished.countDown();
                }
            });
            activity.webView.loadData(withHelper, "text/html", "utf-8");
        });
        assertTrue("capture page did not finish loading within timeout",
            pageFinished.await(PAGE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void awaitNonZeroWebViewWidth(HostActivity activity) throws Exception {
        long deadline = System.currentTimeMillis() + LAYOUT_READINESS_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            int[] widthPx = new int[1];
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                widthPx[0] = activity.webView.getWidth());
            if (widthPx[0] > 0 && readNumber(activity, "window.innerWidth") > 0) {
                return;
            }
            Thread.sleep(READINESS_POLL_INTERVAL_MILLIS);
        }
        assertTrue("web view never obtained a non-zero width and inner width", false);
    }

    private void resetTapRecord(HostActivity activity) throws Exception {
        evaluate(activity, "window.__tapCount=0;window.__tapTargetId='';true");
    }

    private void awaitTapRecorded(HostActivity activity) throws Exception {
        long deadline = System.currentTimeMillis() + READBACK_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (readNumber(activity, "window.__tapCount") >= 1) {
                return;
            }
            Thread.sleep(READINESS_POLL_INTERVAL_MILLIS);
        }
        assertTrue("web view never received the dispatched tap", false);
    }

    private String readString(HostActivity activity, String expression) throws Exception {
        String raw = evaluate(activity, expression);
        if (raw == null || "null".equals(raw)) return "";
        String unquoted = raw;
        if (unquoted.length() >= 2 && unquoted.startsWith("\"") && unquoted.endsWith("\"")) {
            unquoted = unquoted.substring(1, unquoted.length() - 1);
        }
        return unquoted;
    }

    private double readNumber(HostActivity activity, String expression) throws Exception {
        String raw = evaluate(activity, expression);
        if (raw == null || "null".equals(raw) || raw.isEmpty()) return 0;
        try {
            return Double.parseDouble(raw.replace("\"", ""));
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    private String evaluate(HostActivity activity, String expression) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>(null);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            activity.webView.evaluateJavascript(expression, value -> {
                result.set(value);
                latch.countDown();
            }));
        assertTrue("javascript evaluation timed out for: " + expression,
            latch.await(READBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return result.get();
    }
}
