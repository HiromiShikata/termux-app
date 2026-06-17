package com.termux.shared.view;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import com.termux.shared.logger.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ViewUtilsTest {

    private static class StubResources extends Resources {
        private final DisplayMetrics displayMetrics = new DisplayMetrics();

        @SuppressWarnings("deprecation")
        StubResources(float density) {
            super(null, null, null);
            displayMetrics.density = density;
        }

        @Override
        public DisplayMetrics getDisplayMetrics() {
            return displayMetrics;
        }
    }

    private static class StubContext extends ContextWrapper {
        private final Resources resources;

        StubContext(float density) {
            super(null);
            this.resources = new StubResources(density);
        }

        @Override
        public Resources getResources() {
            return resources;
        }
    }

    private static class ChainContext extends ContextWrapper {
        private final Context base;

        ChainContext(Context base) {
            super(null);
            this.base = base;
        }

        @Override
        public Context getBaseContext() {
            return base;
        }
    }

    private static class StubView extends View {
        private ViewGroup.LayoutParams layoutParams;

        StubView(ViewGroup.LayoutParams layoutParams) {
            super(null);
            this.layoutParams = layoutParams;
        }

        @Override
        public ViewGroup.LayoutParams getLayoutParams() {
            return layoutParams;
        }

        @Override
        public void setLayoutParams(ViewGroup.LayoutParams params) {
            this.layoutParams = params;
        }
    }

    private static Rect rect(int left, int top, int right, int bottom) {
        Rect rect = new Rect();
        rect.left = left;
        rect.top = top;
        rect.right = right;
        rect.bottom = bottom;
        return rect;
    }

    private static Point point(int x, int y) {
        Point point = new Point();
        point.x = x;
        point.y = y;
        return point;
    }

    @Before
    public void disableAndroidLogging() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
    }

    @Test
    public void setIsViewUtilsLoggingEnabledTogglesFlag() {
        ViewUtils.setIsViewUtilsLoggingEnabled(true);
        Assert.assertTrue(ViewUtils.VIEW_UTILS_LOGGING_ENABLED);
        ViewUtils.setIsViewUtilsLoggingEnabled(false);
        Assert.assertFalse(ViewUtils.VIEW_UTILS_LOGGING_ENABLED);
    }

    @Test
    public void toRectStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ViewUtils.toRectString(null));
    }

    @Test
    public void toRectStringFormatsCorners() {
        Assert.assertEquals("(1,2), (3,4)", ViewUtils.toRectString(rect(1, 2, 3, 4)));
    }

    @Test
    public void toPointStringReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ViewUtils.toPointString(null));
    }

    @Test
    public void toPointStringFormatsCoordinates() {
        Assert.assertEquals("(5,7)", ViewUtils.toPointString(point(5, 7)));
    }

    @Test
    public void isRectAboveReturnsTrueWhenRectContainsOther() {
        Assert.assertTrue(ViewUtils.isRectAbove(rect(0, 0, 10, 10), rect(2, 2, 8, 8)));
    }

    @Test
    public void isRectAboveReturnsFalseForEmptyBaseRect() {
        Assert.assertFalse(ViewUtils.isRectAbove(rect(0, 0, 0, 0), rect(2, 2, 8, 8)));
    }

    @Test
    public void isRectAboveReturnsFalseWhenBaseStartsRightOfOther() {
        Assert.assertFalse(ViewUtils.isRectAbove(rect(5, 0, 10, 10), rect(0, 0, 8, 8)));
    }

    @Test
    public void isRectAboveReturnsFalseWhenBaseBottomAboveOtherBottom() {
        Assert.assertFalse(ViewUtils.isRectAbove(rect(0, 0, 10, 5), rect(0, 0, 8, 8)));
    }

    @Test
    public void getActivityReturnsNullForNullContext() {
        Assert.assertNull(ViewUtils.getActivity(null));
    }

    @Test
    public void getActivityReturnsNullWhenNoActivityInChain() {
        Assert.assertNull(ViewUtils.getActivity(new ChainContext(new ChainContext(null))));
    }

    @Test
    public void getActivityReturnsActivityWhenContextIsActivity() {
        Activity activity = new Activity();
        Assert.assertSame(activity, ViewUtils.getActivity(activity));
    }

    @Test
    public void pxToDpDividesByDensity() {
        Assert.assertEquals(10f, ViewUtils.pxToDp(new StubContext(2.0f), 20f), 0.0001f);
    }

    @Test
    public void pxToDpWithDensityOneReturnsSameValue() {
        Assert.assertEquals(15f, ViewUtils.pxToDp(new StubContext(1.0f), 15f), 0.0001f);
    }

    @Test
    public void isViewFullyVisibleReturnsFalseForNullView() {
        Assert.assertFalse(ViewUtils.isViewFullyVisible(null, 0));
    }

    @Test
    public void getWindowAndViewRectsReturnsNullForNullView() {
        Assert.assertNull(ViewUtils.getWindowAndViewRects(null, 0));
    }

    @Test
    public void setLayoutMarginsInPixelsIsNoOpWhenParamsAreNotMarginLayoutParams() {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(100, 100);
        StubView view = new StubView(layoutParams);
        ViewUtils.setLayoutMarginsInPixels(view, 1, 2, 3, 4);
        Assert.assertSame(layoutParams, view.getLayoutParams());
    }
}
