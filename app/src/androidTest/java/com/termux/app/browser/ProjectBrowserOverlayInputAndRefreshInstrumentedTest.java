package com.termux.app.browser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;
import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProjectBrowserOverlayInputAndRefreshInstrumentedTest {

    private static final String PROJECT_URL = "https://github.com/HiromiShikata/termux-app";

    @Test
    public void overlayWebViewContainerIsFocusableInTouchModeForHardwareKeyboardInput() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            FrameLayout overlayWebViewContainer =
                activity.findViewById(R.id.project_browser_web_view_container);
            assertNotNull(overlayWebViewContainer);
            assertTrue(overlayWebViewContainer.isFocusable());
            assertTrue(overlayWebViewContainer.isFocusableInTouchMode());
        });
    }

    @Test
    public void overlaySwipeRefreshLayoutIsInflatedAndWrapsTheWebViewContainer() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            SwipeRefreshLayout swipeRefreshLayout =
                activity.findViewById(R.id.project_browser_swipe_refresh);
            FrameLayout overlayWebViewContainer =
                activity.findViewById(R.id.project_browser_web_view_container);
            assertNotNull(swipeRefreshLayout);
            assertNotNull(overlayWebViewContainer);
            assertSame(swipeRefreshLayout, overlayWebViewContainer.getParent());
        });
    }

    @Test
    public void openingAProjectUrlHostsAPerTabWebViewInsideTheContainer() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            controller.openProjectUrl(PROJECT_URL, BrowserViewMode.DESKTOP);

            FrameLayout overlayWebViewContainer =
                activity.findViewById(R.id.project_browser_web_view_container);
            WebView perTabWebView = findFirstWebView(overlayWebViewContainer);
            assertNotNull(perTabWebView);
            assertSame(overlayWebViewContainer, perTabWebView.getParent());
        });
    }

    private static WebView findFirstWebView(FrameLayout container) {
        for (int childIndex = 0; childIndex < container.getChildCount(); childIndex++) {
            View child = container.getChildAt(childIndex);
            if (child instanceof WebView) {
                return (WebView) child;
            }
        }
        return null;
    }
}
