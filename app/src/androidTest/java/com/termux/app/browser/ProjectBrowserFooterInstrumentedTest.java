package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;
import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProjectBrowserFooterInstrumentedTest {

    @Test
    public void footerIconsAreInflatedInsideTheOverlay() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            View footer = activity.findViewById(R.id.project_browser_footer);
            View overviewIcon = activity.findViewById(R.id.project_browser_footer_overview_icon);
            View tdpmConsoleIcon = activity.findViewById(R.id.project_browser_footer_tdpm_console_icon);
            View newIssueIcon = activity.findViewById(R.id.project_browser_footer_new_issue_icon);
            assertNotNull(footer);
            assertNotNull(overviewIcon);
            assertNotNull(tdpmConsoleIcon);
            assertNotNull(newIssueIcon);
        });
    }

    @Test
    public void setProjectContextShowsIconsWithUrlsAndHidesIconsWithout() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            ProjectBrowserOverlayController controller = activity.getProjectBrowserOverlayController();
            assertNotNull(controller);
            controller.setProjectContext(
                "https://github.com/HiromiShikata/termux-app/projects/1", null, "");

            assertEquals(View.VISIBLE,
                activity.findViewById(R.id.project_browser_footer_overview_icon).getVisibility());
            assertEquals(View.GONE,
                activity.findViewById(R.id.project_browser_footer_tdpm_console_icon).getVisibility());
            assertEquals(View.GONE,
                activity.findViewById(R.id.project_browser_footer_new_issue_icon).getVisibility());
        });
    }
}
