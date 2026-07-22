package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;
import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TermuxBrowserControllerInstrumentedTest {

    @Rule
    public final RetryRule retryRule = new RetryRule();

    private static final String LOOPBACK_TAB_URL = "http://127.0.0.1/";

    private static final String SECONDARY_LOOPBACK_TAB_URL = "http://127.0.0.1/second";

    private static final String DETACHED_SESSION_SHELL = "/system/bin/sh";

    @Test
    public void openUrlInNewTabProducesActiveTabAndMakesBrowserVisible() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();
            assertNotNull(browserController);

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);

            BrowserTab activeTab = browserController.getActiveTab();
            assertNotNull(activeTab);
            assertEquals(LOOPBACK_TAB_URL, activeTab.getUrl());
            assertTrue(browserController.isBrowserVisible());
        });
    }

    @Test
    public void showTerminalHidesBrowserAfterOpeningTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);
            assertTrue(browserController.isBrowserVisible());

            browserController.showTerminal();
            assertFalse(browserController.isBrowserVisible());
        });
    }

    @Test
    public void toggleBrowserReturnsToBrowserWhenActiveTabExists() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);
            browserController.showTerminal();
            assertFalse(browserController.isBrowserVisible());

            browserController.toggleBrowser();
            assertTrue(browserController.isBrowserVisible());

            browserController.toggleBrowser();
            assertFalse(browserController.isBrowserVisible());
        });
    }

    @Test
    public void newTabDefaultsToDesktopModeReflectedInResolvedUserAgent() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);

            BrowserTab activeTab = browserController.getActiveTab();
            assertNotNull(activeTab);
            assertTrue(activeTab.isDesktopMode());
            assertEquals(BrowserUserAgent.DESKTOP_USER_AGENT,
                BrowserUserAgent.resolve(activeTab.isDesktopMode(), "default-mobile-user-agent"));

            activeTab.setDesktopMode(false);
            assertEquals(BrowserUserAgent.MOBILE_USER_AGENT,
                BrowserUserAgent.resolve(activeTab.isDesktopMode(), "default-mobile-user-agent"));
        });
    }

    @Test
    public void openingSecondUrlSwitchesActiveTabToTheNewTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);
            browserController.openUrlInNewTab(SECONDARY_LOOPBACK_TAB_URL);

            BrowserTab activeTab = browserController.getActiveTab();
            assertNotNull(activeTab);
            assertEquals(SECONDARY_LOOPBACK_TAB_URL, activeTab.getUrl());
            assertTrue(browserController.isBrowserVisible());
        });
    }

    @Test
    public void openUrlInNewTabIsNoOpWhenNoSessionIsSelected() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(null);
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);

            assertNull(browserController.getActiveTab());
            assertFalse(browserController.isBrowserVisible());
        });
    }

    @Test
    public void drawerBrowserControlViewsAreInflatedAndWired() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.browser_new_tab_button));
            assertNotNull(activity.findViewById(R.id.browser_open_in_chrome_button));
            assertNotNull(activity.findViewById(R.id.browser_send_page_text_button));
            assertNotNull(activity.findViewById(R.id.browser_clear_cache_button));
            assertNotNull(activity.findViewById(R.id.browser_desktop_mode_toggle));
        });
    }

    @Test
    public void sharedSwipeRefreshSpinnerIsResetWhenSwitchingToNewTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.onSessionChanged(newDetachedSession());
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);

            SwipeRefreshLayout swipeRefreshLayout = activity.findViewById(R.id.browser_swipe_refresh);
            assertNotNull(swipeRefreshLayout);
            swipeRefreshLayout.setRefreshing(true);
            assertTrue(swipeRefreshLayout.isRefreshing());

            browserController.openUrlInNewTab(SECONDARY_LOOPBACK_TAB_URL);

            assertFalse(
                "Switching to a new tab must reset the shared pull-to-refresh spinner; "
                    + "the circular indicator must not remain visible on the newly displayed tab",
                swipeRefreshLayout.isRefreshing());
        });
    }

    private static TerminalSession newDetachedSession() {
        return new TerminalSession(DETACHED_SESSION_SHELL, "/", new String[]{DETACHED_SESSION_SHELL},
            new String[0], null, new TermuxTerminalSessionClientBase());
    }
}
