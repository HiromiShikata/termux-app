package com.termux.app.ownercall;

import android.view.View;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogViewportTest {

    private static final int SCREEN_HEIGHT_PIXELS = 2400;
    private static final int TERMINAL_ROW_HEIGHT_PIXELS = 36;

    private static FrameLayout parentHoldingTerminalArea(int left, int top, int right, int bottom,
                                                         int parentHeight) {
        FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
        View terminalArea = new View(RuntimeEnvironment.getApplication());
        parent.addView(terminalArea);
        parent.layout(0, 0, right, parentHeight);
        terminalArea.layout(left, top, right, bottom);
        return parent;
    }

    @Test
    public void measuresTheDialogAgainstTheTerminalAreaOfTheRunningActivity() {
        FrameLayout parent = parentHoldingTerminalArea(0, 0, 1080, 2280, 2400);

        OwnerCallDialogGeometry geometry = OwnerCallDialogViewport.resolve(parent,
            parent.getChildAt(0), SCREEN_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(1080, geometry.getWidthPixels());
        Assert.assertEquals(0, geometry.getLeftMarginPixels());
        Assert.assertEquals(SCREEN_HEIGHT_PIXELS / OwnerCallDialogGeometry.SCREEN_HEIGHT_DIVISOR,
            geometry.getHeightPixels());
        Assert.assertEquals(120
                + OwnerCallDialogGeometry.VISIBLE_TERMINAL_ROWS_BELOW * TERMINAL_ROW_HEIGHT_PIXELS,
            geometry.getBottomMarginPixels());
    }

    @Test
    public void followsTheTerminalPaneWhenItStartsPartWayAcrossTheScreen() {
        FrameLayout parent = parentHoldingTerminalArea(400, 0, 1080, 1000, 1080);

        OwnerCallDialogGeometry geometry = OwnerCallDialogViewport.resolve(parent,
            parent.getChildAt(0), 1080, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(680, geometry.getWidthPixels());
        Assert.assertEquals(400, geometry.getLeftMarginPixels());
    }
}
