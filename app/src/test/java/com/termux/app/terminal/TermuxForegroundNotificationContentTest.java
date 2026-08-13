package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TermuxForegroundNotificationContentTest {

    @Test
    public void aSingleSessionIsCountedInTheSingular() {
        Assert.assertEquals("1 session, 0/1 calls",
            new TermuxForegroundNotificationContent(1, 0, 0, false).getText());
    }

    @Test
    public void severalSessionsAreCountedInThePlural() {
        Assert.assertEquals("19 sessions, 0/19 calls",
            new TermuxForegroundNotificationContent(19, 0, 0, false).getText());
    }

    @Test
    public void withoutASessionThereIsNoFractionToShow() {
        Assert.assertEquals("0 sessions",
            new TermuxForegroundNotificationContent(0, 0, 0, false).getText());
    }

    @Test
    public void runningTasksAreNamedAfterTheSessions() {
        Assert.assertEquals("19 sessions, 1 task, 0/19 calls",
            new TermuxForegroundNotificationContent(19, 1, 0, false).getText());
        Assert.assertEquals("19 sessions, 2 tasks, 0/19 calls",
            new TermuxForegroundNotificationContent(19, 2, 0, false).getText());
    }

    @Test
    public void sessionsWaitingOnTheOwnerAreShownAsAFractionOfAllSessions() {
        Assert.assertEquals("19 sessions, 3/19 calls",
            new TermuxForegroundNotificationContent(19, 0, 3, false).getText());
    }

    @Test
    public void aHeldWakeLockIsNamedLastBecauseItIsAboutPower() {
        Assert.assertEquals("19 sessions, 3/19 calls (wake lock held)",
            new TermuxForegroundNotificationContent(19, 0, 3, true).getText());
    }

    @Test
    public void contentBuiltFromTheSameCountsIsEqual() {
        Assert.assertEquals(new TermuxForegroundNotificationContent(19, 1, 3, true),
            new TermuxForegroundNotificationContent(19, 1, 3, true));
        Assert.assertEquals(new TermuxForegroundNotificationContent(19, 1, 3, true).hashCode(),
            new TermuxForegroundNotificationContent(19, 1, 3, true).hashCode());
    }

    @Test
    public void contentDifferingInAnyCountIsNotEqual() {
        TermuxForegroundNotificationContent content =
            new TermuxForegroundNotificationContent(19, 1, 3, true);

        Assert.assertNotEquals(content, new TermuxForegroundNotificationContent(20, 1, 3, true));
        Assert.assertNotEquals(content, new TermuxForegroundNotificationContent(19, 2, 3, true));
        Assert.assertNotEquals(content, new TermuxForegroundNotificationContent(19, 1, 4, true));
        Assert.assertNotEquals(content, new TermuxForegroundNotificationContent(19, 1, 3, false));
    }
}
