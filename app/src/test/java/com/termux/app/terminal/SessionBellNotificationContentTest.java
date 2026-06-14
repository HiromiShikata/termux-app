package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellNotificationContentTest {

    @Test
    public void contentTextIncludesSessionNameWhenPresent() {
        Assert.assertEquals("Session \"claude\" rang the bell",
            SessionBellNotificationContent.contentText("claude"));
    }

    @Test
    public void contentTextTrimsSessionName() {
        Assert.assertEquals("Session \"claude\" rang the bell",
            SessionBellNotificationContent.contentText("  claude  "));
    }

    @Test
    public void contentTextFallsBackWhenNameNull() {
        Assert.assertEquals("A background session rang the bell",
            SessionBellNotificationContent.contentText(null));
    }

    @Test
    public void contentTextFallsBackWhenNameBlank() {
        Assert.assertEquals("A background session rang the bell",
            SessionBellNotificationContent.contentText("   "));
    }

    @Test
    public void notificationIdIsStableForSameHandle() {
        int first = SessionBellNotificationContent.notificationId(1340, "handle-a");
        int second = SessionBellNotificationContent.notificationId(1340, "handle-a");
        Assert.assertEquals(first, second);
    }

    @Test
    public void notificationIdStaysWithinSlotRange() {
        int id = SessionBellNotificationContent.notificationId(1340, "any-handle");
        Assert.assertTrue(id >= 1340);
        Assert.assertTrue(id < 1340 + SessionBellNotificationContent.NOTIFICATION_ID_SLOTS);
    }

    @Test
    public void notificationIdReturnsBaseWhenHandleNull() {
        Assert.assertEquals(1340, SessionBellNotificationContent.notificationId(1340, null));
    }

    @Test
    public void notificationIdDiffersForDifferentHandles() {
        int a = SessionBellNotificationContent.notificationId(1340, "handle-a");
        int b = SessionBellNotificationContent.notificationId(1340, "handle-b");
        Assert.assertNotEquals(a, b);
    }
}
