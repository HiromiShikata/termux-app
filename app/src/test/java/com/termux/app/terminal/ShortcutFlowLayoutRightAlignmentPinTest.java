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
public class ShortcutFlowLayoutRightAlignmentPinTest {

    @Test
    public void currentLayoutRightAlignsTheRightmostChildToTheRightPaddingEdge() {
        Context context = RuntimeEnvironment.getApplication();
        ShortcutFlowLayout layout = new ShortcutFlowLayout(context);
        layout.setPadding(7, 0, 11, 0);
        View first = fixedSizeChild(context, 13, 20);
        View second = fixedSizeChild(context, 19, 20);
        layout.addView(first);
        layout.addView(second);

        int width = 100;
        layout.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, width, 20);

        Assert.assertSame(first, layout.getChildAt(0));
        Assert.assertSame(second, layout.getChildAt(1));
        Assert.assertEquals(width - layout.getPaddingRight(), second.getRight());
    }

    private static View fixedSizeChild(Context context, int width, int height) {
        View child = new View(context);
        child.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        return child;
    }
}
