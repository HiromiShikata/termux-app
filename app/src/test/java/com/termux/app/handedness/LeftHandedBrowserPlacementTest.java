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
        Assert.assertEquals(Gravity.END | Gravity.TOP, actions.getGravity());
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

    @Test
    public void leftHandedBrowserPlacementProjectOverviewActions() {
        View root = inflateActivityLayout();
        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        LinearLayout actions = root.findViewById(R.id.browser_project_overview_actions);

        Assert.assertEquals(Gravity.BOTTOM | Gravity.START,
            ((FrameLayout.LayoutParams) actions.getLayoutParams()).gravity);
        Assert.assertEquals(Gravity.START | Gravity.TOP, actions.getGravity());
        assertChildren(actions, R.id.browser_open_all_tasks_button,
            R.id.browser_open_first_ten_tasks_button);
    }

    @Test
    public void leftHandedBrowserPlacementTabBar() {
        View root = inflateActivityLayout();
        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        assertChildren((LinearLayout) root.findViewById(R.id.browser_tab_bar),
            R.id.browser_tab_bar_project_actions, R.id.browser_tab_strip_scroll);
        assertChildren((LinearLayout) root.findViewById(R.id.browser_tab_bar_project_actions),
            R.id.browser_tab_bar_new_issue_button, R.id.browser_tab_bar_tdpm_console_button,
            R.id.browser_tab_bar_overview_button);
    }

    @Test
    public void leftHandedBrowserPlacementFindBar() {
        View root = inflateActivityLayout();
        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        assertChildren((LinearLayout) root.findViewById(R.id.browser_find_bar),
            R.id.browser_find_close_button, R.id.browser_find_prev_button,
            R.id.browser_find_next_button, R.id.browser_find_match_counter,
            R.id.browser_find_query_input);
    }

    @Test
    public void leftHandedBrowserPlacementDrawer() {
        View root = inflateActivityLayout();
        DrawerLayout drawerLayout = root.findViewById(R.id.drawer_layout);
        TabDrawerEdge.apply(drawerLayout, Handedness.LEFT_HANDED);
        View drawer = root.findViewById(R.id.right_drawer);

        Assert.assertEquals(Gravity.START, TabDrawerEdge.currentGravity(drawerLayout));
        Assert.assertEquals(Gravity.START, ((DrawerLayout.LayoutParams) drawer.getLayoutParams()).gravity);
        Assert.assertFalse(drawerLayout.isDrawerOpen(Gravity.START));
    }

    @Test
    public void leftHandedBrowserPlacementTabsListRow() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)), leftHandedSource());

        View row = controller.getView(0, null, new FrameLayout(RuntimeEnvironment.getApplication()));

        assertChildren((ViewGroup) row, R.id.browser_tab_close_button, R.id.browser_tab_favicon,
            R.id.browser_tab_content);
    }

    @Test
    public void leftHandedBrowserPlacementFaviconStripCloseButton() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        new BrowserTabFaviconStripController(scrollView, container,
            new RecordingSelectionListener(), leftHandedSource()).update(Arrays.asList(tab), tab);

        View closeButton = container.getChildAt(0).findViewById(R.id.browser_tab_strip_close_button);
        Assert.assertEquals(Gravity.TOP | Gravity.START,
            ((FrameLayout.LayoutParams) closeButton.getLayoutParams()).gravity);
    }

    @Test
    public void leftHandedBrowserPlacementExistingFaviconStripRows() {
        View root = inflateActivityLayout();
        HorizontalScrollView scrollView = root.findViewById(R.id.browser_tab_strip_scroll);
        LinearLayout container = root.findViewById(R.id.browser_tab_strip_container);
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        new BrowserTabFaviconStripController(scrollView, container,
            new RecordingSelectionListener()).update(Arrays.asList(tab), tab);
        new BrowserTabStripHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        View closeButton = container.getChildAt(0).findViewById(R.id.browser_tab_strip_close_button);
        Assert.assertEquals(Gravity.TOP | Gravity.START,
            ((FrameLayout.LayoutParams) closeButton.getLayoutParams()).gravity);
    }

    @Test
    public void leftHandedBrowserPlacementActionIconRow() {
        View root = inflateActivityLayout();
        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        assertChildren((LinearLayout) root.findViewById(R.id.browser_action_icon_row),
            R.id.browser_find_in_page_button, R.id.browser_clear_cache_button,
            R.id.browser_send_screenshot_button, R.id.browser_send_page_text_button,
            R.id.browser_open_in_chrome_button, R.id.browser_bookmark_toggle_button,
            R.id.browser_new_tab_button, R.id.browser_desktop_mode_toggle);
    }

    @Test
    public void leftHandedBrowserPlacementPartAppliesAllStaticControls() {
        View root = inflateActivityLayout();
        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        assertChildren((LinearLayout) root.findViewById(R.id.browser_tab_bar),
            R.id.browser_tab_bar_project_actions, R.id.browser_tab_strip_scroll);
        assertChildren((LinearLayout) root.findViewById(R.id.browser_find_bar),
            R.id.browser_find_close_button, R.id.browser_find_prev_button,
            R.id.browser_find_next_button, R.id.browser_find_match_counter,
            R.id.browser_find_query_input);
        Assert.assertEquals(Gravity.START, TabDrawerEdge.currentGravity(
            (DrawerLayout) root.findViewById(R.id.drawer_layout)));
    }

    @Test
    public void leftHandedBrowserPlacementExistingTabsListRow() {
        View root = inflateActivityLayout();
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        MutableHandednessSource source = new MutableHandednessSource(Handedness.RIGHT_HANDED);
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)), source);
        View row = controller.getView(0, null, new FrameLayout(RuntimeEnvironment.getApplication()));
        ((android.widget.ListView) root.findViewById(R.id.browser_tabs_list)).setAdapter(controller);
        source.handedness = Handedness.LEFT_HANDED;

        new BrowserTabsListHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        row = controller.getView(0, row, new FrameLayout(RuntimeEnvironment.getApplication()));

        assertChildren((ViewGroup) row, R.id.browser_tab_close_button, R.id.browser_tab_favicon,
            R.id.browser_tab_content);
    }

    @Test
    public void restoreRightHandedBrowserPlacementAfterLeftHandedApplication() {
        View root = inflateActivityLayout();
        PlacementSnapshot before = PlacementSnapshot.capture(root);
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        HorizontalScrollView scrollView = root.findViewById(R.id.browser_tab_strip_scroll);
        LinearLayout strip = root.findViewById(R.id.browser_tab_strip_container);
        BrowserTabFaviconStripController stripController = new BrowserTabFaviconStripController(
            scrollView, strip, new RecordingSelectionListener());
        stripController.update(Arrays.asList(tab), tab);
        int faviconCloseGravity = ((FrameLayout.LayoutParams) strip.getChildAt(0)
            .findViewById(R.id.browser_tab_strip_close_button).getLayoutParams()).gravity;
        BrowserTabsListViewController tabsController = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)));
        ((android.widget.ListView) root.findViewById(R.id.browser_tabs_list)).setAdapter(tabsController);
        View row = tabsController.getView(0, null, new FrameLayout(RuntimeEnvironment.getApplication()));

        new BrowserHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new BrowserTabStripHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new BrowserTabsListHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        tabsController.getView(0, row, new FrameLayout(RuntimeEnvironment.getApplication()));
        new BrowserHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        new BrowserTabStripHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        new BrowserTabsListHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);

        before.assertMatches(root);
        Assert.assertEquals(faviconCloseGravity, ((FrameLayout.LayoutParams) strip.getChildAt(0)
            .findViewById(R.id.browser_tab_strip_close_button).getLayoutParams()).gravity);
        assertChildren((ViewGroup) row, R.id.browser_tab_favicon, R.id.browser_tab_content,
            R.id.browser_tab_close_button);
    }

    @Test
    public void restoreRightHandedBrowserPlacementCreatesRightHandedRowAfterSwitchingBack() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        MutableHandednessSource source = new MutableHandednessSource(Handedness.RIGHT_HANDED);
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)), source);
        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        View row = controller.getView(0, null, parent);
        source.handedness = Handedness.LEFT_HANDED;
        row = controller.getView(0, row, parent);
        source.handedness = Handedness.RIGHT_HANDED;
        row = controller.getView(0, row, parent);

        assertChildren((ViewGroup) row, R.id.browser_tab_favicon, R.id.browser_tab_content,
            R.id.browser_tab_close_button);
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private static HandednessSource leftHandedSource() {
        return () -> Handedness.LEFT_HANDED;
    }

    private static final class MutableHandednessSource implements HandednessSource {
        private Handedness handedness;

        private MutableHandednessSource(Handedness handedness) {
            this.handedness = handedness;
        }

        @Override
        public Handedness current() {
            return handedness;
        }
    }

    private static final class PlacementSnapshot {
        private final int[] projectOverviewOrder;
        private final int projectOverviewLayoutGravity;
        private final int projectOverviewGravity;
        private final int[] tabBarOrder;
        private final int[] tabBarActionsOrder;
        private final int[] findBarOrder;
        private final int drawerGravity;
        private final int[] actionIconOrder;

        private PlacementSnapshot(View root) {
            LinearLayout projectOverview = root.findViewById(R.id.browser_project_overview_actions);
            projectOverviewOrder = childIds(projectOverview);
            projectOverviewLayoutGravity = ((FrameLayout.LayoutParams)
                projectOverview.getLayoutParams()).gravity;
            projectOverviewGravity = projectOverview.getGravity();
            tabBarOrder = childIds((ViewGroup) root.findViewById(R.id.browser_tab_bar));
            tabBarActionsOrder = childIds((ViewGroup) root.findViewById(
                R.id.browser_tab_bar_project_actions));
            findBarOrder = childIds((ViewGroup) root.findViewById(R.id.browser_find_bar));
            drawerGravity = ((DrawerLayout.LayoutParams) root.findViewById(R.id.right_drawer)
                .getLayoutParams()).gravity;
            actionIconOrder = childIds((ViewGroup) root.findViewById(R.id.browser_action_icon_row));
        }

        private static PlacementSnapshot capture(View root) {
            return new PlacementSnapshot(root);
        }

        private void assertMatches(View root) {
            LinearLayout projectOverview = root.findViewById(R.id.browser_project_overview_actions);
            Assert.assertArrayEquals(projectOverviewOrder, childIds(projectOverview));
            Assert.assertEquals(projectOverviewLayoutGravity, ((FrameLayout.LayoutParams)
                projectOverview.getLayoutParams()).gravity);
            Assert.assertEquals(projectOverviewGravity, projectOverview.getGravity());
            Assert.assertArrayEquals(tabBarOrder, childIds((ViewGroup) root.findViewById(
                R.id.browser_tab_bar)));
            Assert.assertArrayEquals(tabBarActionsOrder, childIds((ViewGroup) root.findViewById(
                R.id.browser_tab_bar_project_actions)));
            Assert.assertArrayEquals(findBarOrder, childIds((ViewGroup) root.findViewById(
                R.id.browser_find_bar)));
            Assert.assertEquals(drawerGravity, ((DrawerLayout.LayoutParams)
                root.findViewById(R.id.right_drawer).getLayoutParams()).gravity);
            Assert.assertArrayEquals(actionIconOrder, childIds((ViewGroup) root.findViewById(
                R.id.browser_action_icon_row)));
        }

        private static int[] childIds(ViewGroup parent) {
            int[] ids = new int[parent.getChildCount()];
            for (int index = 0; index < parent.getChildCount(); index++) {
                ids[index] = parent.getChildAt(index).getId();
            }
            return ids;
        }
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
