package com.termux.app.browser;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

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
}
