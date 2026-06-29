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
    public void tabChipStaysSingleFaviconSizedWithCloseButtonOverlaidAtTheCorner() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        controllerFor(listener, scrollView, container)
            .update(Arrays.asList(firstTab, secondTab), firstTab);

        View item = container.getChildAt(0);
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        item.measure(unspecified, unspecified);

        View faviconColumn = item.findViewById(R.id.browser_tab_strip_favicon);
        View faviconColumnParent = (View) faviconColumn.getParent();
        int faviconColumnWidth = faviconColumnParent.getMeasuredWidth();

        Assert.assertEquals(
            "Tab chip must stay the size of a single favicon column, not a widened chip",
            faviconColumnWidth, item.getMeasuredWidth());

        float density = context.getResources().getDisplayMetrics().density;
        int seventySixDpInPx = Math.round(76f * density);
        Assert.assertTrue(
            "Tab chip width must stay well below the previously widened 76dp footprint",
            item.getMeasuredWidth() < seventySixDpInPx);

        FrameLayout.LayoutParams faviconColumnParams =
            (FrameLayout.LayoutParams) faviconColumnParent.getLayoutParams();
        Assert.assertEquals(
            "Favicon column must not be pushed to the start edge with a gap",
            0, faviconColumnParams.getMarginStart());
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
    public void closeButtonCornerTargetReachesMaterialMinimumAnchoredTopRightOverFavicon() {
        Rect closeButtonBounds = new Rect(17, 0, 32, 15);
        int targetSizePx = 48;

        Rect cornerTarget = BrowserTabFaviconStripController
            .computeCloseButtonCornerTarget(closeButtonBounds, targetSizePx);

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
        Assert.assertEquals(
            "Corner touch target must extend left from the corner by the Material minimum",
            closeButtonBounds.right - targetSizePx, cornerTarget.left);
    }

    @Test
    public void closeButtonCornerTargetExtendsLeftByMaterialMinimumIndependentOfChipWidth() {
        Rect closeButtonBounds = new Rect(20, 0, 35, 15);
        int targetSizePx = 48;

        Rect cornerTarget = BrowserTabFaviconStripController
            .computeCloseButtonCornerTarget(closeButtonBounds, targetSizePx);

        Assert.assertEquals(
            "Corner touch target left edge must extend a full Material minimum from the corner",
            closeButtonBounds.right - targetSizePx, cornerTarget.left);
        Assert.assertEquals(
            "Corner touch target width must equal the Material minimum",
            targetSizePx, cornerTarget.width());
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
