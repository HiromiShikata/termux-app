package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellNotificationClearDecisionTest {

    @Test
    public void clearsWhenStillCurrentAndStillPending() {
        Assert.assertTrue(SessionBellNotificationClearDecision.shouldClear(true, true));
    }

    @Test
    public void keepsMarkWhenSessionIsNoLongerCurrent() {
        Assert.assertFalse(SessionBellNotificationClearDecision.shouldClear(false, true));
    }

    @Test
    public void doesNotClearWhenNotificationAlreadyGone() {
        Assert.assertFalse(SessionBellNotificationClearDecision.shouldClear(true, false));
    }

    @Test
    public void doesNotClearWhenNeitherCurrentNorPending() {
        Assert.assertFalse(SessionBellNotificationClearDecision.shouldClear(false, false));
    }
}
