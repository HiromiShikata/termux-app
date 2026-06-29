package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class AllSessionsStatuslineScanGateTest {

    private static final boolean HAS_DATA = true;
    private static final boolean NO_DATA = false;

    @Test
    public void firstScanOfASessionIsAllowed() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L, HAS_DATA));
    }

    @Test
    public void unchangedContentVersionIsSkippedWhenTheSessionHasData() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 10L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 10L, HAS_DATA));
    }

    @Test
    public void changedContentVersionIsScannedAgain() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 10L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 10L, HAS_DATA));
        Assert.assertTrue(gate.shouldScan("session-a", 11L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 11L, HAS_DATA));
    }

    @Test
    public void eachSessionIsTrackedIndependently() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 5L, HAS_DATA));
        Assert.assertTrue(gate.shouldScan("session-b", 5L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 5L, HAS_DATA));
        Assert.assertTrue(gate.shouldScan("session-b", 6L, HAS_DATA));
    }

    @Test
    public void forgettingASessionAllowsItToScanAgain() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 5L, HAS_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 5L, HAS_DATA));
        gate.forget("session-a");
        Assert.assertTrue(gate.shouldScan("session-a", 5L, HAS_DATA));
    }

    @Test
    public void markScannedRecordsTheVersionSoTheNextGatedTickSkipsTheUnchangedScreen() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        gate.markScanned("session-a", 7L);
        Assert.assertFalse(gate.shouldScan("session-a", 7L, HAS_DATA));
    }

    @Test
    public void markScannedDoesNotSkipWhenTheVersionChangesAfterAForcedPass() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        gate.markScanned("session-a", 7L);
        Assert.assertTrue(gate.shouldScan("session-a", 8L, HAS_DATA));
    }

    @Test
    public void markScannedOnAFirstSeenSessionStillLetsAForcedPassRecordItsVersion() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 3L, HAS_DATA));
        gate.markScanned("session-a", 3L);
        Assert.assertFalse(gate.shouldScan("session-a", 3L, HAS_DATA));
    }

    @Test
    public void sessionWithNoStoredDataIsScannedEvenWhenContentVersionIsUnchanged() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 4L, NO_DATA));
        Assert.assertTrue(gate.shouldScan("session-a", 4L, NO_DATA));
        Assert.assertTrue(gate.shouldScan("session-a", 4L, NO_DATA));
    }

    @Test
    public void sessionStopsBeingForcedOnceItGainsDataAndContentVersionIsUnchanged() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        Assert.assertTrue(gate.shouldScan("session-a", 4L, NO_DATA));
        Assert.assertFalse(gate.shouldScan("session-a", 4L, HAS_DATA));
    }
}
