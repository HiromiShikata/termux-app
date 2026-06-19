package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TermuxSessionsListViewControllerOutputActivityTest {

    @Test
    public void showsIndicatorWhenOutputActivityRecordedWithoutBell() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");

        Assert.assertTrue(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, "handle"));
    }

    @Test
    public void showsIndicatorWhenBellRecordedWithoutOutputActivity() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        Assert.assertTrue(TermuxSessionsListViewController.hasNewActivityIndicator(
            1_000L, outputActivityStore, "handle"));
    }

    @Test
    public void hidesIndicatorWhenNeitherBellNorOutputActivityRecorded() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, "handle"));
    }

    @Test
    public void hidesIndicatorWhenSessionHandleIsNull() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");

        Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, null));
    }

    @Test
    public void indicatorStaysOffAcrossRepeatedRendersAfterClearWithoutNewEvent() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");

        outputActivityStore.clearOutputActivity("handle");

        for (int renderPass = 0; renderPass < 5; renderPass++) {
            Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
                null, outputActivityStore, "handle"));
        }
    }

    @Test
    public void indicatorReappearsWhenANewBackgroundEventArrivesAfterClear() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();
        outputActivityStore.markOutputActivity("handle");
        outputActivityStore.clearOutputActivity("handle");
        Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, "handle"));

        SessionOutputActivityMarker.markBackgroundOutputActivity(
            outputActivityStore, "current-handle", "handle");

        Assert.assertTrue(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, "handle"));
    }

    @Test
    public void currentSessionIsNotMarkedSoIndicatorStaysOff() {
        SessionOutputActivityStore outputActivityStore = new SessionOutputActivityStore();

        SessionOutputActivityMarker.markBackgroundOutputActivity(
            outputActivityStore, "current-handle", "current-handle");

        Assert.assertFalse(TermuxSessionsListViewController.hasNewActivityIndicator(
            null, outputActivityStore, "current-handle"));
    }
}
