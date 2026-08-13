package com.termux.app.ownercall;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogViewportTest {

    private static final int SCREEN_HEIGHT_PIXELS = 2400;
    private static final int ROOT_HEIGHT_PIXELS = 2280;
    private static final int TERMINAL_ROW_HEIGHT_PIXELS = 36;

    @Test
    public void takesTheDialogWidthAndPositionFromTheTerminalAreaOnScreen() {
        OwnerCallDialogGeometry geometry = geometryFor(0, 0, 1080, 1900);

        Assert.assertEquals(1080, geometry.getWidthPixels());
        Assert.assertEquals(0, geometry.getLeftMarginPixels());
        Assert.assertEquals(SCREEN_HEIGHT_PIXELS / 4, geometry.getHeightPixels());
    }

    @Test
    public void keepsTheAgreedTerminalRowsVisibleBelowWhateverSitsUnderTheTerminalArea() {
        OwnerCallDialogGeometry geometry = geometryFor(0, 0, 1080, 1900);

        int pixelsBelowTheTerminalArea = ROOT_HEIGHT_PIXELS - 1900;
        Assert.assertEquals(pixelsBelowTheTerminalArea + 5 * TERMINAL_ROW_HEIGHT_PIXELS,
            geometry.getBottomMarginPixels());
    }

    @Test
    public void followsTheTerminalPaneWhenItOccupiesOnlyPartOfTheScreen() {
        OwnerCallDialogGeometry geometry = geometryFor(620, 0, 460, 1900);

        Assert.assertEquals(620, geometry.getLeftMarginPixels());
        Assert.assertEquals(460, geometry.getWidthPixels());
    }

    private static OwnerCallDialogGeometry geometryFor(int terminalLeft, int terminalTop,
                                                       int terminalWidth, int terminalBottom) {
        Context context = RuntimeEnvironment.getApplication();
        RelativeLayout dialogParent = new RelativeLayout(context);
        View terminalArea = new View(context);
        dialogParent.addView(terminalArea);
        dialogParent.layout(0, 0, 1080, ROOT_HEIGHT_PIXELS);
        terminalArea.layout(terminalLeft, terminalTop, terminalLeft + terminalWidth, terminalBottom);

        return OwnerCallDialogViewport.resolve(dialogParent, terminalArea, SCREEN_HEIGHT_PIXELS,
            TERMINAL_ROW_HEIGHT_PIXELS);
    }
}
