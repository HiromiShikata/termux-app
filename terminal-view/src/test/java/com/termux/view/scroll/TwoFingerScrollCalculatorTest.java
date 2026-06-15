package com.termux.view.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TwoFingerScrollCalculatorTest {

    private static final float LINE_SPACING = 30f;

    @Test
    public void notTrackingUntilStarted() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        assertFalse(calculator.isTracking());
        calculator.start(100f);
        assertTrue(calculator.isTracking());
        calculator.stop();
        assertFalse(calculator.isTracking());
    }

    @Test
    public void noMovementProducesZeroRows() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(200f);
        assertEquals(0, calculator.consumeRows(200f, LINE_SPACING));
    }

    @Test
    public void fingersMovingUpProducePositiveRows() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(300f);
        assertEquals(2, calculator.consumeRows(240f, LINE_SPACING));
    }

    @Test
    public void fingersMovingDownProduceNegativeRows() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(300f);
        assertEquals(-2, calculator.consumeRows(360f, LINE_SPACING));
    }

    @Test
    public void subRowMovementAccumulatesAcrossEvents() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(100f);
        assertEquals(0, calculator.consumeRows(80f, LINE_SPACING));
        assertEquals(1, calculator.consumeRows(60f, LINE_SPACING));
    }

    @Test
    public void remainderIsResetOnStart() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(100f);
        calculator.consumeRows(85f, LINE_SPACING);
        calculator.start(500f);
        assertEquals(0, calculator.consumeRows(490f, LINE_SPACING));
        assertEquals(1, calculator.consumeRows(470f, LINE_SPACING));
    }

    @Test
    public void nonPositiveLineSpacingProducesZeroRows() {
        TwoFingerScrollCalculator calculator = new TwoFingerScrollCalculator();
        calculator.start(100f);
        assertEquals(0, calculator.consumeRows(0f, 0f));
        assertEquals(0, calculator.consumeRows(0f, -10f));
    }
}
