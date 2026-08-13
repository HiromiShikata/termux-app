package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class NotificationRepostDecisionTest {

    private final NotificationRepostDecision<TermuxForegroundNotificationContent> mDecision =
        new NotificationRepostDecision<>();

    private static TermuxForegroundNotificationContent content(int sessionCount, int taskCount,
                                                               int pendingCallSessionCount,
                                                               boolean wakeLockHeld) {
        return new TermuxForegroundNotificationContent(sessionCount, taskCount, pendingCallSessionCount,
            wakeLockHeld);
    }

    @Test
    public void theFirstContentIsAlwaysPosted() {
        Assert.assertTrue("nothing has been shown yet, so there is nothing to compare against",
            mDecision.isNeededFor(content(19, 0, 3, false)));
    }

    @Test
    public void contentIdenticalToWhatIsAlreadyShownIsNotPostedAgain() {
        mDecision.isNeededFor(content(19, 0, 3, false));

        Assert.assertFalse("shell output does not change the session count, the task count, the pending"
                + " call count or the wake lock, so reposting spends binder calls on the main thread to"
                + " redraw exactly what is already on screen",
            mDecision.isNeededFor(content(19, 0, 3, false)));
    }

    @Test
    public void everyFieldThatAppearsInTheNotificationTriggersARepostOnItsOwn() {
        mDecision.isNeededFor(content(19, 0, 3, false));
        Assert.assertTrue(mDecision.isNeededFor(content(20, 0, 3, false)));
        Assert.assertTrue(mDecision.isNeededFor(content(20, 1, 3, false)));
        Assert.assertTrue(mDecision.isNeededFor(content(20, 1, 4, false)));
        Assert.assertTrue(mDecision.isNeededFor(content(20, 1, 4, true)));
        Assert.assertFalse(mDecision.isNeededFor(content(20, 1, 4, true)));
    }

    @Test
    public void contentThatReturnsToAnEarlierValueIsPostedAgain() {
        mDecision.isNeededFor(content(19, 0, 3, false));
        mDecision.isNeededFor(content(19, 0, 4, false));

        Assert.assertTrue("only what is currently on screen may be skipped, not everything ever shown",
            mDecision.isNeededFor(content(19, 0, 3, false)));
    }

    @Test
    public void aCountOnlyNotificationIsTrackedByTheSameDecision() {
        NotificationRepostDecision<Integer> pendingCallDecision = new NotificationRepostDecision<>();

        Assert.assertTrue(pendingCallDecision.isNeededFor(3));
        Assert.assertFalse(pendingCallDecision.isNeededFor(3));
        Assert.assertTrue(pendingCallDecision.isNeededFor(0));
        Assert.assertFalse("cancelling twice is two binder calls for one empty screen",
            pendingCallDecision.isNeededFor(0));
    }
}
