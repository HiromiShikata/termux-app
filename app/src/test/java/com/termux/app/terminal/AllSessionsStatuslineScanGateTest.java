package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class AllSessionsStatuslineScanGateTest {

    @Test
    public void firstScanOfASessionIsAllowed() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L));
    }

    @Test
    public void unchangedContentVersionIsSkipped() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L));
        Assert.assertFalse(gate.shouldScan("session-a", 10L));
        Assert.assertFalse(gate.shouldScan("session-a", 10L));
    }

    @Test
    public void changedContentVersionIsScannedAgain() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L));
        Assert.assertFalse(gate.shouldScan("session-a", 10L));
        Assert.assertTrue(gate.shouldScan("session-a", 11L));
        Assert.assertFalse(gate.shouldScan("session-a", 11L));
    }

    @Test
    public void eachSessionIsTrackedIndependently() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 5L));
        Assert.assertTrue(gate.shouldScan("session-b", 5L));
        Assert.assertFalse(gate.shouldScan("session-a", 5L));
        Assert.assertTrue(gate.shouldScan("session-b", 6L));
    }

    @Test
    public void forgettingASessionAllowsItToScanAgain() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 5L));
        Assert.assertFalse(gate.shouldScan("session-a", 5L));
        gate.forget("session-a");
        Assert.assertTrue(gate.shouldScan("session-a", 5L));
    }
}
