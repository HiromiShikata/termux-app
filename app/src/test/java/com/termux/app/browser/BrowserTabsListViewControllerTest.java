package com.termux.app.browser;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.theme.NightMode;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserTabsListViewControllerTest {

    private static final String SESSION = "session";

    private static final class RecordingSelectionListener implements BrowserTabSelectionListener {
        final List<BrowserTab> openedTabs = new ArrayList<>();
        final List<BrowserTab> closedTabs = new ArrayList<>();
        @Nullable BrowserTab activeTab;

        @Override
        public void openTab(@NonNull BrowserTab tab) {
            openedTabs.add(tab);
        }

        @Override
        public void closeTab(@NonNull BrowserTab tab) {
            closedTabs.add(tab);
        }

        @Override
        public void promptNewTab() {
        }

        @Nullable
        @Override
        public BrowserTab getActiveTab() {
            return activeTab;
        }
    }

    private View rowViewFor(BrowserTabsListViewController controller, int position) {
        Context context = RuntimeEnvironment.getApplication();
        return controller.getView(position, null, new FrameLayout(context));
    }

    @Test
    public void tappingTabRowOpensThatTab() {
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = firstTab;

        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, Arrays.asList(firstTab, secondTab));

        View secondRow = rowViewFor(controller, 1);
        secondRow.performClick();

        Assert.assertEquals(1, listener.openedTabs.size());
        Assert.assertSame(secondTab, listener.openedTabs.get(0));
    }

    @Test
    public void tabRowExposesAClickListener() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = tab;

        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, new ArrayList<>(Arrays.asList(tab)));

        View row = rowViewFor(controller, 0);

        Assert.assertTrue(row.hasOnClickListeners());
    }

    @Test
    public void closeButtonClosesThatTabWithoutOpeningIt() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = tab;

        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, new ArrayList<>(Arrays.asList(tab)));

        View row = rowViewFor(controller, 0);
        ImageButton closeButton = row.findViewById(R.id.browser_tab_close_button);
        closeButton.performClick();

        Assert.assertEquals(1, listener.closedTabs.size());
        Assert.assertSame(tab, listener.closedTabs.get(0));
        Assert.assertTrue(listener.openedTabs.isEmpty());
    }

    @Test
    public void rowFaviconIsPopulatedForEachTab() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = tab;
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, new ArrayList<>(Arrays.asList(tab)));

        ImageView faviconView = rowViewFor(controller, 0).findViewById(R.id.browser_tab_favicon);

        Assert.assertNotNull(faviconView.getDrawable());
    }

    @Test
    public void onItemClickOpensTabAtPosition() {
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();

        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, Arrays.asList(firstTab, secondTab));

        controller.onItemClick(null, null, 1, 1L);

        Assert.assertEquals(1, listener.openedTabs.size());
        Assert.assertSame(secondTab, listener.openedTabs.get(0));
    }

    @After
    public void resetAppNightMode() {
        NightMode.setAppNightMode(NightMode.SYSTEM.getName());
    }

    @Test
    public void rowCountEqualsCurrentSessionTabCount() {
        List<BrowserTab> tabs = Arrays.asList(
            new BrowserTab(SESSION, "https://one.example/"),
            new BrowserTab(SESSION, "https://two.example/"),
            new BrowserTab(SESSION, "https://three.example/"));
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(), tabs);

        Assert.assertEquals(tabs.size(), controller.getCount());
    }

    @Test
    public void emptyTitleTabRendersUrlInsteadOfBlankLabel() {
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        tab.setTitle("");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = tab;
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, new ArrayList<>(Arrays.asList(tab)));

        TextView titleView = rowViewFor(controller, 0).findViewById(R.id.browser_tab_title);

        Assert.assertEquals("https://only.example/", titleView.getText().toString());
        Assert.assertFalse(titleView.getText().toString().isEmpty());
    }

    @Test
    public void inactiveRowTextIsReadableInLightMode() {
        NightMode.setAppNightMode(NightMode.FALSE.getName());
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)));

        View row = rowViewFor(controller, 0);
        TextView titleView = row.findViewById(R.id.browser_tab_title);
        TextView urlView = row.findViewById(R.id.browser_tab_url);

        Assert.assertEquals(Color.BLACK, titleView.getCurrentTextColor());
        Assert.assertEquals(Color.BLACK, urlView.getCurrentTextColor());
    }

    @Test
    public void inactiveRowTextIsReadableInDarkMode() {
        NightMode.setAppNightMode(NightMode.TRUE.getName());
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), new RecordingSelectionListener(),
            new ArrayList<>(Arrays.asList(tab)));

        View row = rowViewFor(controller, 0);
        TextView titleView = row.findViewById(R.id.browser_tab_title);
        TextView urlView = row.findViewById(R.id.browser_tab_url);

        Assert.assertEquals(Color.WHITE, titleView.getCurrentTextColor());
        Assert.assertEquals(Color.WHITE, urlView.getCurrentTextColor());
    }

    @Test
    public void activeRowTitleUsesHighlightColor() {
        NightMode.setAppNightMode(NightMode.TRUE.getName());
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");
        RecordingSelectionListener listener = new RecordingSelectionListener();
        listener.activeTab = tab;
        BrowserTabsListViewController controller = new BrowserTabsListViewController(
            RuntimeEnvironment.getApplication(), listener, new ArrayList<>(Arrays.asList(tab)));

        TextView titleView = rowViewFor(controller, 0).findViewById(R.id.browser_tab_title);

        Assert.assertEquals(0xFF03A9F4, titleView.getCurrentTextColor());
        Assert.assertNotEquals(Color.WHITE, titleView.getCurrentTextColor());
    }
}
