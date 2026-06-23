package com.termux.app.browser;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
public class BrowserTabFaviconStripControllerTest {

    private static final String SESSION = "session";

    private static final class RecordingSelectionListener implements BrowserTabSelectionListener {
        final List<BrowserTab> openedTabs = new ArrayList<>();
        final List<BrowserTab> closedTabs = new ArrayList<>();
        int newTabPromptCount = 0;
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
            newTabPromptCount++;
        }

        @Nullable
        @Override
        public BrowserTab getActiveTab() {
            return activeTab;
        }
    }

    private BrowserTabFaviconStripController controllerFor(RecordingSelectionListener listener,
                                                           HorizontalScrollView scrollView,
                                                           LinearLayout container) {
        return new BrowserTabFaviconStripController(scrollView, container, listener);
    }

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    @Test
    public void stripIsHiddenWhenOnlyOneTabExists() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        controllerFor(listener, scrollView, container).update(Arrays.asList(tab), tab);

        Assert.assertEquals(View.GONE, scrollView.getVisibility());
        Assert.assertEquals(0, container.getChildCount());
    }

    @Test
    public void stripShowsTabItemsAndAddButtonWhenMultipleTabsExist() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        Assert.assertEquals(View.VISIBLE, scrollView.getVisibility());
        Assert.assertEquals(3, container.getChildCount());
    }

    @Test
    public void tappingStripItemOpensThatTab() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        container.getChildAt(1).performClick();

        Assert.assertEquals(1, listener.openedTabs.size());
        Assert.assertSame(secondTab, listener.openedTabs.get(0));
    }

    @Test
    public void tappingCloseButtonClosesThatTabWithoutOpeningIt() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        View secondItem = container.getChildAt(1);
        View closeButton = secondItem.findViewById(R.id.browser_tab_strip_close_button);
        closeButton.performClick();

        Assert.assertEquals(1, listener.closedTabs.size());
        Assert.assertSame(secondTab, listener.closedTabs.get(0));
        Assert.assertTrue(listener.openedTabs.isEmpty());
    }

    @Test
    public void tappingAddButtonPromptsForNewTabWithoutOpeningOrClosingTabs() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        View addItem = container.getChildAt(2);
        View addButton = addItem.findViewById(R.id.browser_tab_strip_add_button);
        addButton.performClick();

        Assert.assertEquals(1, listener.newTabPromptCount);
        Assert.assertTrue(listener.openedTabs.isEmpty());
        Assert.assertTrue(listener.closedTabs.isEmpty());
    }

    @Test
    public void activeIndicatorIsVisibleOnlyForActiveTab() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), secondTab);

        View firstIndicator = container.getChildAt(0)
            .findViewById(R.id.browser_tab_strip_active_indicator);
        View secondIndicator = container.getChildAt(1)
            .findViewById(R.id.browser_tab_strip_active_indicator);

        Assert.assertEquals(View.INVISIBLE, firstIndicator.getVisibility());
        Assert.assertEquals(View.VISIBLE, secondIndicator.getVisibility());
    }

    @Test
    public void faviconImageIsPopulatedForEachTabItem() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        List<BrowserTab> tabs = new ArrayList<>(Arrays.asList(
            new BrowserTab(SESSION, "https://first.example/"),
            new BrowserTab(SESSION, "https://second.example/")));

        controllerFor(listener, scrollView, container).update(tabs, tabs.get(0));

        ImageView firstFavicon = container.getChildAt(0)
            .findViewById(R.id.browser_tab_strip_favicon);
        ImageView secondFavicon = container.getChildAt(1)
            .findViewById(R.id.browser_tab_strip_favicon);

        Assert.assertNotNull(firstFavicon.getDrawable());
        Assert.assertNotNull(secondFavicon.getDrawable());
    }
}
