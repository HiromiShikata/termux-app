package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OwnerCallDialogGeometryTest {

    @Test
    public void makesTheDialogAQuarterOfTheScreenHeight() {
        OwnerCallDialogGeometry geometry =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 0, 24);

        assertEquals(606, geometry.getHeightPixels());
    }

    @Test
    public void makesTheDialogAsWideAsTheTerminalArea() {
        OwnerCallDialogGeometry portrait =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 0, 24);
        OwnerCallDialogGeometry landscape =
            OwnerCallDialogGeometry.resolve(1080, 1035, 1389, 0, 24);

        assertEquals(1080, portrait.getWidthPixels());
        assertEquals(1389, landscape.getWidthPixels());
    }

    @Test
    public void startsTheDialogAtTheLeftEdgeOfTheTerminalArea() {
        OwnerCallDialogGeometry portrait =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 0, 24);
        OwnerCallDialogGeometry landscape =
            OwnerCallDialogGeometry.resolve(1080, 1035, 1389, 0, 24);

        assertEquals(0, portrait.getLeftMarginPixels());
        assertEquals(1035, landscape.getLeftMarginPixels());
    }

    @Test
    public void leavesFiveTerminalRowsVisibleBelowTheDialog() {
        OwnerCallDialogGeometry geometry =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 0, 48);

        assertEquals(48 * 5, geometry.getBottomMarginPixels());
    }

    @Test
    public void keepsTheFiveRowsAboveWhateverSitsBelowTheTerminalArea() {
        OwnerCallDialogGeometry geometry =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 279, 48);

        assertEquals(279 + 48 * 5, geometry.getBottomMarginPixels());
    }

    @Test
    public void appliesTheSameRuleInLandscapeAsInPortrait() {
        OwnerCallDialogGeometry portrait =
            OwnerCallDialogGeometry.resolve(2424, 0, 1080, 100, 24);
        OwnerCallDialogGeometry landscape =
            OwnerCallDialogGeometry.resolve(1080, 1035, 1389, 100, 24);

        assertEquals(portrait.getBottomMarginPixels(), landscape.getBottomMarginPixels());
        assertEquals(1080 / 4, landscape.getHeightPixels());
    }

    @Test
    public void treatsNegativeMeasurementsAsZero() {
        OwnerCallDialogGeometry geometry =
            OwnerCallDialogGeometry.resolve(-1, -1, -1, -1, -1);

        assertEquals(0, geometry.getWidthPixels());
        assertEquals(0, geometry.getHeightPixels());
        assertEquals(0, geometry.getLeftMarginPixels());
        assertEquals(0, geometry.getBottomMarginPixels());
    }
}
