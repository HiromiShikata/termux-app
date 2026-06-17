package com.termux.shared.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ViewUtilsDimensionTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void dpToPxScalesByDisplayDensity() {
        Context context = context();
        float density = context.getResources().getDisplayMetrics().density;
        Assert.assertEquals(16f * density, ViewUtils.dpToPx(context, 16f), 0.001f);
    }

    @Test
    public void pxToDpIsInverseOfDensityScaling() {
        Context context = context();
        float density = context.getResources().getDisplayMetrics().density;
        Assert.assertEquals(48f / density, ViewUtils.pxToDp(context, 48f), 0.001f);
    }

    @Test
    public void dpToPxAndPxToDpRoundTrip() {
        Context context = context();
        float pixels = ViewUtils.dpToPx(context, 24f);
        Assert.assertEquals(24f, ViewUtils.pxToDp(context, pixels), 0.001f);
    }

    @Test
    public void setLayoutMarginsInPixelsUpdatesMarginLayoutParams() {
        View view = new View(context());
        view.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewUtils.setLayoutMarginsInPixels(view, 1, 2, 3, 4);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        Assert.assertEquals(1, params.leftMargin);
        Assert.assertEquals(2, params.topMargin);
        Assert.assertEquals(3, params.rightMargin);
        Assert.assertEquals(4, params.bottomMargin);
    }

    @Test
    public void setLayoutMarginsInPixelsIgnoresNonMarginLayoutParams() {
        View view = new View(context());
        view.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewUtils.setLayoutMarginsInPixels(view, 5, 6, 7, 8);

        Assert.assertFalse(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams);
    }

    @Test
    public void setLayoutMarginsInDpConvertsThroughDensity() {
        Context context = context();
        View view = new View(context);
        view.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewUtils.setLayoutMarginsInDp(view, 10, 0, 0, 0);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        Assert.assertEquals((int) ViewUtils.dpToPx(context, 10), params.leftMargin);
    }

    @Test
    public void getWindowAndViewRectsReturnsNullForNullView() {
        Assert.assertNull(ViewUtils.getWindowAndViewRects(null, 0));
    }

    @Test
    public void getWindowAndViewRectsReturnsNullForHiddenView() {
        View view = new View(context());
        Assert.assertNull(ViewUtils.getWindowAndViewRects(view, 0));
    }

    @Test
    public void isViewFullyVisibleReturnsFalseForNullView() {
        Assert.assertFalse(ViewUtils.isViewFullyVisible(null, 0));
    }
}
