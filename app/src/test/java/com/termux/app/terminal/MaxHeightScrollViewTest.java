package com.termux.app.terminal;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MaxHeightScrollViewTest {

    private static final int MAX_HEIGHT_PIXELS = 68;

    private static final int WIDTH_PIXELS = 300;

    private Activity activity() {
        return Robolectric.buildActivity(Activity.class).create().get();
    }

    private TextView tallChild(Activity activity, int requestedHeightPixels) {
        TextView child = new TextView(activity);
        child.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, requestedHeightPixels));
        child.setMinimumHeight(requestedHeightPixels);
        return child;
    }

    private MaxHeightScrollView scrollViewWithChildHeight(int childHeightPixels) {
        Activity activity = activity();
        MaxHeightScrollView scrollView = new MaxHeightScrollView(activity);
        scrollView.setMaxScrollHeightPixels(MAX_HEIGHT_PIXELS);
        scrollView.addView(tallChild(activity, childHeightPixels));
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        scrollView.layout(0, 0, scrollView.getMeasuredWidth(), scrollView.getMeasuredHeight());
        return scrollView;
    }

    @Test
    public void capsMeasuredHeightToTheConfiguredMaximumWhenContentIsTaller() {
        MaxHeightScrollView scrollView = scrollViewWithChildHeight(MAX_HEIGHT_PIXELS * 50);

        Assert.assertEquals(MAX_HEIGHT_PIXELS, scrollView.getMeasuredHeight());
    }

    @Test
    public void manyTallEntriesStayCappedAndRemainInternallyScrollable() {
        int veryTallContent = MAX_HEIGHT_PIXELS * 200;
        MaxHeightScrollView scrollView = scrollViewWithChildHeight(veryTallContent);

        Assert.assertEquals(MAX_HEIGHT_PIXELS, scrollView.getMeasuredHeight());

        int maxScrollAmount = scrollView.getChildAt(0).getMeasuredHeight()
            - scrollView.getMeasuredHeight();
        Assert.assertTrue("content taller than the cap must remain scrollable internally",
            maxScrollAmount > 0);
        Assert.assertEquals(veryTallContent, scrollView.getChildAt(0).getMeasuredHeight());
    }

    @Test
    public void shortContentKeepsItsNaturalHeightBelowTheCap() {
        int shortContent = MAX_HEIGHT_PIXELS / 4;
        MaxHeightScrollView scrollView = scrollViewWithChildHeight(shortContent);

        int childHeight = scrollView.getChildAt(0).getMeasuredHeight();
        Assert.assertTrue("short content must stay below the cap so the footer is not padded out",
            childHeight < MAX_HEIGHT_PIXELS);
        Assert.assertEquals("the footer must wrap short content rather than expand to the cap",
            childHeight, scrollView.getMeasuredHeight());
    }

    @Test
    public void withoutAMaxHeightTheViewWrapsItsContent() {
        Activity activity = activity();
        MaxHeightScrollView scrollView = new MaxHeightScrollView(activity);
        int tall = MAX_HEIGHT_PIXELS * 10;
        scrollView.addView(tallChild(activity, tall));
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        Assert.assertEquals(tall, scrollView.getMeasuredHeight());
    }
}
