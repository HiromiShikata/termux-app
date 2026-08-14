package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogGeometryTest {

    private static final int SCREEN_HEIGHT_PIXELS = 2400;
    private static final int TERMINAL_ROW_HEIGHT_PIXELS = 36;
    private static final int TOOLBAR_HEIGHT_PIXELS = 120;

    @Test
    public void occupiesAQuarterOfTheScreenHeight() {
        OwnerCallDialogGeometry geometry = OwnerCallDialogGeometry.resolve(SCREEN_HEIGHT_PIXELS, 0,
            1080, TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(SCREEN_HEIGHT_PIXELS / OwnerCallDialogGeometry.SCREEN_HEIGHT_DIVISOR,
            geometry.getHeightPixels());
    }

    @Test
    public void spansTheFullScreenWidthWhenTheTerminalDoes() {
        OwnerCallDialogGeometry geometry = OwnerCallDialogGeometry.resolve(SCREEN_HEIGHT_PIXELS, 0,
            1080, TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(1080, geometry.getWidthPixels());
        Assert.assertEquals(0, geometry.getLeftMarginPixels());
    }

    @Test
    public void spansOnlyTheTerminalPaneWhenTheSessionListSharesTheScreen() {
        OwnerCallDialogGeometry geometry = OwnerCallDialogGeometry.resolve(1080, 1035, 1389,
            TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(1389, geometry.getWidthPixels());
        Assert.assertEquals(1035, geometry.getLeftMarginPixels());
        Assert.assertEquals(1080 / OwnerCallDialogGeometry.SCREEN_HEIGHT_DIVISOR,
            geometry.getHeightPixels());
    }

    @Test
    public void sitsHighEnoughToLeaveFiveTerminalRowsReadableBelowIt() {
        OwnerCallDialogGeometry geometry = OwnerCallDialogGeometry.resolve(SCREEN_HEIGHT_PIXELS, 0,
            1080, TOOLBAR_HEIGHT_PIXELS, TERMINAL_ROW_HEIGHT_PIXELS);

        Assert.assertEquals(TOOLBAR_HEIGHT_PIXELS
                + OwnerCallDialogGeometry.VISIBLE_TERMINAL_ROWS_BELOW * TERMINAL_ROW_HEIGHT_PIXELS,
            geometry.getBottomMarginPixels());
    }

    @Test
    public void takesNoNegativeMeasurementFromAnUnmeasuredTerminal() {
        OwnerCallDialogGeometry geometry =
            OwnerCallDialogGeometry.resolve(-1, -1, -1, -1, -1);

        Assert.assertEquals(0, geometry.getWidthPixels());
        Assert.assertEquals(0, geometry.getHeightPixels());
        Assert.assertEquals(0, geometry.getLeftMarginPixels());
        Assert.assertEquals(0, geometry.getBottomMarginPixels());
    }
}
