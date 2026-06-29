package com.termux.app.browser;

import android.content.Context;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.FrameLayout;
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
    public void stripAndAddButtonAreShownWhenOnlyOneTabExists() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        controllerFor(listener, scrollView, container).update(Arrays.asList(tab), tab);

        Assert.assertEquals(View.VISIBLE, scrollView.getVisibility());
        Assert.assertEquals(2, container.getChildCount());

        View addItem = container.getChildAt(1);
        View addButton = addItem.findViewById(R.id.browser_tab_strip_add_button);
        addButton.performClick();

        Assert.assertEquals(1, listener.newTabPromptCount);
        Assert.assertTrue(listener.openedTabs.isEmpty());
        Assert.assertTrue(listener.closedTabs.isEmpty());
    }

    @Test
    public void tappingSingleTabItemOpensThatTab() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab tab = new BrowserTab(SESSION, "https://only.example/");

        controllerFor(listener, scrollView, container).update(Arrays.asList(tab), tab);

        container.getChildAt(0).performClick();

        Assert.assertEquals(1, listener.openedTabs.size());
        Assert.assertSame(tab, listener.openedTabs.get(0));
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
    public void closeButtonIsSmallCornerTargetSeparatedFromTabSelectArea() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        View item = container.getChildAt(0);
        ImageView faviconView = item.findViewById(R.id.browser_tab_strip_favicon);
        ImageView closeButton = item.findViewById(R.id.browser_tab_strip_close_button);

        Assert.assertTrue(
            "Close button must be a smaller target than the favicon select area",
            closeButton.getLayoutParams().width < faviconView.getLayoutParams().width);

        float density = context.getResources().getDisplayMetrics().density;
        int fifteenDpInPx = Math.round(15f * density);
        Assert.assertTrue(
            "Close button must be at least one step larger than the prior 13dp target",
            closeButton.getLayoutParams().width >= fifteenDpInPx);

        FrameLayout.LayoutParams closeParams =
            (FrameLayout.LayoutParams) closeButton.getLayoutParams();
        int horizontalGravity = closeParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int verticalGravity = closeParams.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        Assert.assertEquals(
            "Close button must stay pinned to the end (right) edge",
            Gravity.RIGHT, horizontalGravity);
        Assert.assertEquals(
            "Close button must stay pinned to the top edge",
            Gravity.TOP, verticalGravity);
        Assert.assertEquals(
            "Close button must sit flush in the top-right corner with no top margin",
            0, closeParams.topMargin);
        Assert.assertEquals(
            "Close button must sit flush in the top-right corner with no end margin",
            0, closeParams.getMarginEnd());
        Assert.assertTrue(
            "Close button glyph padding must shrink the drawn icon",
            closeButton.getPaddingLeft() > 0);
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

    @Test
    public void closeButtonCornerTargetReachesMaterialMinimumAnchoredTopRightClearOfFavicon() {
        Rect closeButtonBounds = new Rect(170, 0, 185, 15);
        Rect faviconBounds = new Rect(9, 9, 41, 41);
        int targetSizePx = 48;

        Rect cornerTarget = BrowserTabFaviconStripController
            .computeCloseButtonCornerTarget(closeButtonBounds, faviconBounds, targetSizePx);

        Assert.assertTrue(
            "Corner touch target width must reach the Material minimum",
            cornerTarget.width() >= targetSizePx);
        Assert.assertTrue(
            "Corner touch target height must reach the Material minimum",
            cornerTarget.height() >= targetSizePx);
        Assert.assertEquals(
            "Corner touch target must stay pinned to the close button right edge",
            closeButtonBounds.right, cornerTarget.right);
        Assert.assertEquals(
            "Corner touch target must stay pinned to the close button top edge",
            closeButtonBounds.top, cornerTarget.top);
        Assert.assertTrue(
            "Corner touch target must not cover the favicon glyph select area",
            cornerTarget.left >= faviconBounds.right);
    }

    @Test
    public void closeButtonCornerTargetExtendsToFaviconEdgeWhenGapWiderThanMinimum() {
        Rect closeButtonBounds = new Rect(300, 0, 315, 15);
        Rect faviconBounds = new Rect(9, 9, 41, 41);
        int targetSizePx = 48;

        Rect cornerTarget = BrowserTabFaviconStripController
            .computeCloseButtonCornerTarget(closeButtonBounds, faviconBounds, targetSizePx);

        Assert.assertEquals(
            "Corner touch target left edge must clear the favicon and fill the empty gap",
            faviconBounds.right, cornerTarget.left);
        Assert.assertTrue(
            "Widened corner touch target still meets the Material minimum",
            cornerTarget.width() >= targetSizePx);
    }

    @Test
    public void eachTabItemReceivesACornerCloseTouchDelegateAfterLayout() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST);
        container.measure(widthSpec, heightSpec);
        container.layout(0, 0, container.getMeasuredWidth(), container.getMeasuredHeight());

        View firstItem = container.getChildAt(0);
        TouchDelegate firstDelegate = firstItem.getTouchDelegate();
        Assert.assertNotNull(
            "Each tab chip must expose a corner close touch delegate", firstDelegate);

        View secondItem = container.getChildAt(1);
        Assert.assertNotNull(
            "Each tab chip must expose a corner close touch delegate",
            secondItem.getTouchDelegate());
    }
}
