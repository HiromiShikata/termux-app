package com.termux.app.terminal;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShortcutFlowLayoutStartAlignmentTest {

    @Test
    public void leftHandedAlignmentStartsEachRowAtTheLeftPaddingEdge() {
        ShortcutFlowLayout layout = layout();
        layout.setStartAligned(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);

        Assert.assertTrue(layout.isStartAligned());
        Assert.assertEquals(layout.getPaddingLeft(), layout.getChildAt(0).getLeft());
        Assert.assertEquals(layout.getPaddingLeft() + 13, layout.getChildAt(1).getLeft());
    }

    @Test
    public void leftHandedAlignmentThenRightHandedRestoresThePinnedPositions() {
        ShortcutFlowLayout layout = layout();
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);
        int firstLeft = layout.getChildAt(0).getLeft();
        int secondLeft = layout.getChildAt(1).getLeft();
        layout.setStartAligned(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);
        layout.setStartAligned(false);
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);

        Assert.assertFalse(layout.isStartAligned());
        Assert.assertEquals(firstLeft, layout.getChildAt(0).getLeft());
        Assert.assertEquals(secondLeft, layout.getChildAt(1).getLeft());
    }

    @Test
    public void applyingLeftHandedAlignmentTwiceIsIdempotent() {
        ShortcutFlowLayout layout = layout();
        layout.setStartAligned(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);
        int firstLeft = layout.getChildAt(0).getLeft();
        int secondLeft = layout.getChildAt(1).getLeft();
        layout.setStartAligned(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 100, 20);

        Assert.assertEquals(firstLeft, layout.getChildAt(0).getLeft());
        Assert.assertEquals(secondLeft, layout.getChildAt(1).getLeft());
    }

    private static ShortcutFlowLayout layout() {
        Context context = RuntimeEnvironment.getApplication();
        ShortcutFlowLayout layout = new ShortcutFlowLayout(context);
        layout.setPadding(7, 0, 11, 0);
        layout.addView(fixedSizeChild(context, 13, 20));
        layout.addView(fixedSizeChild(context, 19, 20));
        layout.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        return layout;
    }

    private static View fixedSizeChild(Context context, int width, int height) {
        View child = new View(context);
        child.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        return child;
    }
}
