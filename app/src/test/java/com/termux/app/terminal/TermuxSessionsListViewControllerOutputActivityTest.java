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
}
