package com.termux.app.browser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.webkit.WebView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;
import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProjectBrowserOverlayInputAndRefreshInstrumentedTest {

    @Test
    public void overlayWebViewIsFocusableInTouchModeForHardwareKeyboardInput() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            WebView overlayWebView = activity.findViewById(R.id.project_browser_web_view);
            assertNotNull(overlayWebView);
            assertTrue(overlayWebView.isFocusable());
            assertTrue(overlayWebView.isFocusableInTouchMode());
        });
    }

    @Test
    public void overlaySwipeRefreshLayoutIsInflatedAndWrapsWebView() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            SwipeRefreshLayout swipeRefreshLayout =
                activity.findViewById(R.id.project_browser_swipe_refresh);
            WebView overlayWebView = activity.findViewById(R.id.project_browser_web_view);
            assertNotNull(swipeRefreshLayout);
            assertNotNull(overlayWebView);
            assertTrue(overlayWebView.getParent() == swipeRefreshLayout);
        });
    }
}
