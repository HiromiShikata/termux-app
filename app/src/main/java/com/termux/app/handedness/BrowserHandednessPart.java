package com.termux.app.handedness;

import android.view.View;
import android.widget.LinearLayout;

import com.termux.R;

import java.util.Arrays;

public final class BrowserHandednessPart implements HandednessLayoutPart {

    private final LinearLayout projectOverviewActions;
    private final LinearLayout tabBar;
    private final LinearLayout tabBarProjectActions;
    private final LinearLayout findBar;
    private final LinearLayout actionIconRow;
    private final LinearLayoutGravityMirror gravityMirror = new LinearLayoutGravityMirror();

    public BrowserHandednessPart(View activityRoot) {
        projectOverviewActions = activityRoot.findViewById(R.id.browser_project_overview_actions);
        tabBar = activityRoot.findViewById(R.id.browser_tab_bar);
        tabBarProjectActions = activityRoot.findViewById(R.id.browser_tab_bar_project_actions);
        findBar = activityRoot.findViewById(R.id.browser_find_bar);
        actionIconRow = activityRoot.findViewById(R.id.browser_action_icon_row);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        HandednessGravity.applyToFrameChild(projectOverviewActions, handedness);
        gravityMirror.apply(projectOverviewActions, handedness);
        HandednessChildOrder.apply(tabBar, handedness, Arrays.asList(
            tabBarProjectActions, tabBar.findViewById(R.id.browser_tab_strip_scroll)));
        HandednessChildOrder.apply(tabBarProjectActions, handedness, Arrays.asList(
            tabBarProjectActions.findViewById(R.id.browser_tab_bar_new_issue_button),
            tabBarProjectActions.findViewById(R.id.browser_tab_bar_tdpm_console_button),
            tabBarProjectActions.findViewById(R.id.browser_tab_bar_overview_button)));
        HandednessChildOrder.apply(findBar, handedness, Arrays.asList(
            findBar.findViewById(R.id.browser_find_close_button),
            findBar.findViewById(R.id.browser_find_prev_button),
            findBar.findViewById(R.id.browser_find_next_button),
            findBar.findViewById(R.id.browser_find_match_counter),
            findBar.findViewById(R.id.browser_find_query_input)));
        HandednessChildOrder.apply(actionIconRow, handedness, Arrays.asList(
            actionIconRow.findViewById(R.id.browser_find_in_page_button),
            actionIconRow.findViewById(R.id.browser_clear_cache_button),
            actionIconRow.findViewById(R.id.browser_send_screenshot_button),
            actionIconRow.findViewById(R.id.browser_send_page_text_button),
            actionIconRow.findViewById(R.id.browser_open_in_chrome_button),
            actionIconRow.findViewById(R.id.browser_bookmark_toggle_button),
            actionIconRow.findViewById(R.id.browser_new_tab_button),
            actionIconRow.findViewById(R.id.browser_desktop_mode_toggle)));
    }

}
