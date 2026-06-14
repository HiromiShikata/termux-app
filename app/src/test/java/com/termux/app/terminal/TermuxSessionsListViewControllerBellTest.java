package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TermuxSessionsListViewControllerBellTest {

    @Test
    public void showsBellIndicatorWhenBellRecordedForSessionHandle() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        store.recordBell("handle-with-bell", 1_000L);

        Assert.assertEquals(Long.valueOf(1_000L),
            TermuxSessionsListViewController.bellArrivalTimeMillis(store, "handle-with-bell"));
    }

    @Test
    public void hidesBellIndicatorWhenNoBellRecordedForSessionHandle() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        store.recordBell("other-handle", 1_000L);

        Assert.assertNull(
            TermuxSessionsListViewController.bellArrivalTimeMillis(store, "handle-without-bell"));
    }

    @Test
    public void hidesBellIndicatorWhenSessionHandleIsNull() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();

        Assert.assertNull(TermuxSessionsListViewController.bellArrivalTimeMillis(store, null));
    }

    @Test
    public void hidesBellIndicatorAfterBellClearedForSession() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        store.recordBell("handle", 1_000L);
        store.clearBell("handle");

        Assert.assertNull(TermuxSessionsListViewController.bellArrivalTimeMillis(store, "handle"));
    }
}
