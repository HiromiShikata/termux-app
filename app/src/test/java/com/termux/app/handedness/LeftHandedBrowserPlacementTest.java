package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.browser.BrowserTab;
import com.termux.app.browser.BrowserTabFaviconStripController;
import com.termux.app.browser.BrowserTabSelectionListener;
import com.termux.app.browser.BrowserTabsListViewController;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class LeftHandedBrowserPlacementTest {

    private static final String SESSION = "session";

    @Test
    public void rightHandedBrowserPlacementProjectOverviewActions() {
        View root = inflateActivityLayout();
        LinearLayout actions = root.findViewById(R.id.browser_project_overview_actions);

        Assert.assertEquals(Gravity.BOTTOM | Gravity.END,
            ((FrameLayout.LayoutParams) actions.getLayoutParams()).gravity);
        Assert.assertEquals(Gravity.END, actions.getGravity());
        assertChildren(actions, R.id.browser_open_all_tasks_button,
            R.id.browser_open_first_ten_tasks_button);
    }

    @Test
    public void rightHandedBrowserPlacementTabBar() {
        View root = inflateActivityLayout();
        LinearLayout tabBar = root.findViewById(R.id.browser_tab_bar);
        LinearLayout actions = root.findViewById(R.id.browser_tab_bar_project_actions);

        assertChildren(tabBar, R.id.browser_tab_strip_scroll,
            R.id.browser_tab_bar_project_actions);
        assertChildren(actions, R.id.browser_tab_bar_overview_button,
            R.id.browser_tab_bar_tdpm_console_button, R.id.browser_tab_bar_new_issue_button);
    }

    @Test
    public void rightHandedBrowserPlacementFindBar() {
        View root = inflateActivityLayout();
        assertChildren((LinearLayout) root.findViewById(R.id.browser_find_bar),
            R.id.browser_find_query_input, R.id.browser_find_match_counter,
            R.id.browser_find_prev_button, R.id.browser_find_next_button,
            R.id.browser_find_close_button);
    }

    @Test
    public void rightHandedBrowserPlacementDrawer() {
        View root = inflateActivityLayout();
        DrawerLayout drawerLayout = root.findViewById(R.id.drawer_layout);
        View drawer = root.findViewById(R.id.right_drawer);

        Assert.assertEquals(Gravity.END, ((DrawerLayout.LayoutParams) drawer.getLayoutParams()).gravity);
        Assert.assertFalse(drawerLayout.isDrawerOpen(Gravity.END));
    }

    @Test
    public void rightHandedBrowserPlacementTabsListRow() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)));

        View row = controller.getView(0, null, new FrameLayout(RuntimeEnvironment.getApplication()));

        assertChildren((ViewGroup) row, R.id.browser_tab_favicon, R.id.browser_tab_content,
            R.id.browser_tab_close_button);
    }

    @Test
    public void rightHandedBrowserPlacementFaviconStripCloseButton() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        new BrowserTabFaviconStripController(scrollView, container, new RecordingSelectionListener())
            .update(Arrays.asList(tab), tab);

        View closeButton = container.getChildAt(0).findViewById(R.id.browser_tab_strip_close_button);
        Assert.assertEquals(Gravity.TOP | Gravity.END,
            ((FrameLayout.LayoutParams) closeButton.getLayoutParams()).gravity);
    }

    @Test
    public void rightHandedBrowserPlacementActionIconRow() {
        View root = inflateActivityLayout();
        assertChildren((LinearLayout) root.findViewById(R.id.browser_action_icon_row),
            R.id.browser_desktop_mode_toggle, R.id.browser_new_tab_button,
            R.id.browser_bookmark_toggle_button, R.id.browser_open_in_chrome_button,
            R.id.browser_send_page_text_button, R.id.browser_send_screenshot_button,
            R.id.browser_clear_cache_button, R.id.browser_find_in_page_button);
    }

    private static void assertChildren(ViewGroup parent, int... ids) {
        Assert.assertEquals(ids.length, parent.getChildCount());
        for (int index = 0; index < ids.length; index++) {
            Assert.assertEquals(ids[index], parent.getChildAt(index).getId());
        }
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

    private static final class RecordingSelectionListener implements BrowserTabSelectionListener {
        @Override
        public void openTab(@NonNull BrowserTab tab) {
        }

        @Override
        public void closeTab(@NonNull BrowserTab tab) {
        }

        @Override
        public void promptNewTab() {
        }

        @Nullable
        @Override
        public BrowserTab getActiveTab() {
            return null;
        }
    }
}
