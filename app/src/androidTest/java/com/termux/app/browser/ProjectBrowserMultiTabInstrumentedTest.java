package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;
import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProjectBrowserMultiTabInstrumentedTest {

    private static final String FIRST_URL = "https://github.com/HiromiShikata/termux-app";

    private static final String SECOND_URL = "https://github.com/HiromiShikata";

    @Test
    public void openingProjectUrlShowsTheOverlayWithASingleTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            assertNotNull(controller);

            controller.openProjectUrl(FIRST_URL, BrowserViewMode.DESKTOP);

            assertTrue(controller.isVisible());
            assertNotNull(controller.getActiveTab());
            assertEquals(FIRST_URL, controller.getActiveTab().getUrl());

            HorizontalScrollView stripScroll =
                activity.findViewById(R.id.project_browser_tab_strip_scroll);
            LinearLayout stripContainer =
                activity.findViewById(R.id.project_browser_tab_strip_container);
            assertEquals(View.VISIBLE, stripScroll.getVisibility());
            assertEquals(2, stripContainer.getChildCount());
        });
    }

    @Test
    public void openInNewTabAddsATabRatherThanReplacingTheCurrentPage() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            controller.openProjectUrl(FIRST_URL, BrowserViewMode.DESKTOP);
            BrowserTab firstTab = controller.getActiveTab();

            controller.openProjectUrlInNewTab(SECOND_URL, BrowserViewMode.DESKTOP);

            BrowserTab activeTab = controller.getActiveTab();
            assertNotNull(activeTab);
            assertNotSame(firstTab, activeTab);
            assertEquals(SECOND_URL, activeTab.getUrl());

            LinearLayout stripContainer =
                activity.findViewById(R.id.project_browser_tab_strip_container);
            assertEquals(3, stripContainer.getChildCount());
        });
    }

    @Test
    public void closingTheActiveTabFallsBackToTheRemainingTab() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            controller.openProjectUrl(FIRST_URL, BrowserViewMode.DESKTOP);
            BrowserTab firstTab = controller.getActiveTab();
            controller.openProjectUrlInNewTab(SECOND_URL, BrowserViewMode.DESKTOP);
            BrowserTab secondTab = controller.getActiveTab();

            controller.closeTab(secondTab);

            assertTrue(controller.isVisible());
            assertSame(firstTab, controller.getActiveTab());
        });
    }

    @Test
    public void closingTheLastTabHidesTheOverlay() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            controller.openProjectUrl(FIRST_URL, BrowserViewMode.DESKTOP);
            BrowserTab onlyTab = controller.getActiveTab();

            controller.closeTab(onlyTab);

            assertEquals(false, controller.isVisible());
            assertEquals(null, controller.getActiveTab());
        });
    }
}
