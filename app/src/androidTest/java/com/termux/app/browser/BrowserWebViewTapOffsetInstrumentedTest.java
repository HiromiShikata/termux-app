package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
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

    private static final int TEST_RETRY_ATTEMPTS = 3;

    private static final int TARGET_TOLERANCE_CSS_PX = 24;

    private static final String CAPTURE_PAGE_HTML =
        "<!doctype html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>html,body{margin:0;padding:0;height:100%;width:100%}"
            + "#grid{position:absolute;top:0;left:0;width:100%;height:100%;display:flex}"
            + ".cell{flex:1;height:100%}"
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
            + "window.__innerWidth=0;window.__innerHeight=0;"
            + "function record(x,y){"
            + "window.__tapClientX=x;window.__tapClientY=y;"
            + "window.__innerWidth=window.innerWidth;window.__innerHeight=window.innerHeight;"
            + "var el=document.elementFromPoint(x,y);"
            + "window.__tapTargetId=el?el.id:'';window.__tapCount++;}"
            + "document.addEventListener('click',function(e){record(e.clientX,e.clientY);},true);"
            + "</script></body></html>";

    @Rule
    public final RetryRule retryRule = new RetryRule(TEST_RETRY_ATTEMPTS);

    public static final class HostActivity extends Activity {
        public static volatile boolean sSplitLayout;

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
            BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, settings.getUserAgentString());
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
    public void tapInLandscapeSplitLandsOnTheElementUnderTheTouchPoint() throws Exception {
        measureTapOffset(true, false);
    }

    @Test
    public void tapInFullWidthPortraitLandsOnTheElementUnderTheTouchPoint() throws Exception {
        measureTapOffset(false, false);
    }

    @Test
    public void tapAfterShrinkingBrowserColumnToSplitLandsOnTheElementUnderTheTouchPoint()
            throws Exception {
        measureTapOffset(false, true);
    }

    private void measureTapOffset(boolean splitLayout, boolean shrinkToSplitAfterLoad)
            throws Exception {
        HostActivity.sSplitLayout = splitLayout;
        ActivityScenario<HostActivity> scenario = ActivityScenario.launch(HostActivity.class);
        AtomicReference<HostActivity> ref = new AtomicReference<>();
        scenario.onActivity(ref::set);
        HostActivity activity = ref.get();

        loadCapturePage(activity);
        awaitNonZeroWebViewWidth(activity);

        int fullWidthPx = webViewWidth(activity);
        if (shrinkToSplitAfterLoad) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                LinearLayout.LayoutParams browserParams =
                    (LinearLayout.LayoutParams) activity.browserColumn.getLayoutParams();
                browserParams.weight = 0.5f;
                activity.browserColumn.setLayoutParams(browserParams);
                LinearLayout.LayoutParams terminalParams =
                    (LinearLayout.LayoutParams) activity.terminalColumn.getLayoutParams();
                terminalParams.weight = 0.5f;
                activity.terminalColumn.setLayoutParams(terminalParams);
            });
            awaitWebViewWidthBelow(activity, fullWidthPx);
        }

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

        int tapScreenX = webViewLeftPx + webViewWidthPx * 5 / 6;
        int tapScreenY = webViewTopPx + webViewHeightPx / 2;

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click(tapScreenX, tapScreenY);
        Thread.sleep(TAP_SETTLE_MILLIS);
        awaitTapRecorded(activity);

        String hitId = readString(activity, "window.__tapTargetId");
        double clientX = readNumber(activity, "window.__tapClientX");
        double clientY = readNumber(activity, "window.__tapClientY");
        double innerWidth = readNumber(activity, "window.__innerWidth");

        double localViewX = tapScreenX - webViewLeftPx;
        double pageScale = innerWidth > 0 ? webViewWidthPx / innerWidth : 0;
        double receivedScreenX = clientX * pageScale;
        double offsetPx = receivedScreenX - localViewX;

        String layoutLabel = shrinkToSplitAfterLoad ? "shrunk-to-split"
            : (splitLayout ? "landscape-split" : "portrait-full");
        Log.i(LOG_TAG, "layout=" + layoutLabel
            + " webViewLeftPx=" + webViewLeftPx
            + " webViewWidthPx=" + webViewWidthPx
            + " innerWidthCss=" + innerWidth
            + " dispatchedLocalViewX=" + localViewX
            + " receivedClientX=" + clientX
            + " receivedClientY=" + clientY
            + " receivedScreenX=" + receivedScreenX
            + " tapOffsetPx=" + offsetPx
            + " hitElementId=" + hitId);

        assertEquals(
            "a tap in the right sixth of the " + (splitLayout ? "split" : "full-width")
                + " browser column must activate the 'right' element, but hit '" + hitId
                + "' (receivedClientX=" + clientX + ", innerWidthCss=" + innerWidth
                + ", webViewWidthPx=" + webViewWidthPx + ", tapOffsetPx=" + offsetPx + ")",
            "right", hitId);
        assertTrue(
            "the tap coordinate the page received must map back to the dispatched screen point"
                + " within tolerance (tapOffsetPx=" + offsetPx + ")",
            Math.abs(offsetPx) <= TARGET_TOLERANCE_CSS_PX);
    }

    private void loadCapturePage(HostActivity activity) throws Exception {
        CountDownLatch pageFinished = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    pageFinished.countDown();
                }
            });
            activity.webView.loadData(CAPTURE_PAGE_HTML, "text/html", "utf-8");
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

    private int webViewWidth(HostActivity activity) {
        int[] widthPx = new int[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            widthPx[0] = activity.webView.getWidth());
        return widthPx[0];
    }

    private void awaitWebViewWidthBelow(HostActivity activity, int previousWidthPx) throws Exception {
        long deadline = System.currentTimeMillis() + LAYOUT_READINESS_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            int currentWidthPx = webViewWidth(activity);
            if (currentWidthPx > 0 && currentWidthPx < previousWidthPx) {
                return;
            }
            Thread.sleep(READINESS_POLL_INTERVAL_MILLIS);
        }
        assertTrue("web view width never shrank below " + previousWidthPx
            + " after collapsing the split", false);
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
