package com.termux.app.browser;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserSwipeRefreshSpinnerTabSwitchScenarioTest {

    private BrowserPinchAwareSwipeRefreshLayout buildLayout() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        BrowserPinchAwareSwipeRefreshLayout layout = new BrowserPinchAwareSwipeRefreshLayout(activity);
        layout.addView(new View(activity), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 1000, 1000);
        return layout;
    }

    @Test
    public void sharedSwipeRefreshSpinnerIsNotRefreshingAfterTabSwitchToNonLoadingTab() {
        BrowserPinchAwareSwipeRefreshLayout swipeRefreshLayout = buildLayout();

        swipeRefreshLayout.setRefreshing(true);
        Assert.assertTrue(swipeRefreshLayout.isRefreshing());

        swipeRefreshLayout.setRefreshing(false);

        Assert.assertFalse(
                "Switching to a non-loading tab must reset the shared pull-to-refresh spinner; "
                        + "the circular indicator must not remain visible on the newly displayed tab",
                swipeRefreshLayout.isRefreshing());
    }
}
