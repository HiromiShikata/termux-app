package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class BackgroundOutputScanGateTest {

    @Test
    public void firstScanForSessionAlwaysRuns() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();

        Assert.assertTrue(gate.shouldScan("handle-one", 1L, 1_000L));
    }

    @Test
    public void unchangedContentVersionSkipsScan() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);

        Assert.assertFalse(gate.shouldScan("handle-one", 5L, 5_000L));
    }

    @Test
    public void changedContentVersionRunsScanOnceThrottleIntervalElapsed() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);

        Assert.assertTrue(gate.shouldScan("handle-one", 6L, 1_400L));
    }

    @Test
    public void changedContentVersionWithinThrottleIntervalSkipsScan() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);

        Assert.assertFalse(gate.shouldScan("handle-one", 6L, 1_100L));
    }

    @Test
    public void throttleSkippedChangeIsStillScannedOnTheNextRenderAfterTheInterval() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);
        Assert.assertFalse(gate.shouldScan("handle-one", 6L, 1_100L));

        Assert.assertTrue(gate.shouldScan("handle-one", 6L, 1_500L));
    }

    @Test
    public void distinctSessionsAreGatedIndependently() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);

        Assert.assertTrue(gate.shouldScan("handle-two", 5L, 1_010L));
    }

    @Test
    public void forgottenSessionScansAgainOnNextContent() {
        BackgroundOutputScanGate gate = new BackgroundOutputScanGate();
        gate.shouldScan("handle-one", 5L, 1_000L);
        gate.forget("handle-one");

        Assert.assertTrue(gate.shouldScan("handle-one", 5L, 1_010L));
    }
}
