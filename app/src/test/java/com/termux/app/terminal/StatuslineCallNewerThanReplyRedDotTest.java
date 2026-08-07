package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.TimeZone;

public class StatuslineCallNewerThanReplyRedDotTest {

    private static final String SESSION_NAME = "app";

    private static final long CALL_MILLIS = Instant.parse("2026-08-06T11:45:25Z").toEpochMilli();

    private static final long REPLY_MILLIS = Instant.parse("2026-08-06T11:04:42Z").toEpochMilli();

    private static final long NOW_MILLIS = Instant.parse("2026-08-06T12:05:00Z").toEpochMilli();

    private static final String CURRENT_STATUSLINE =
        "  call:2026-08-06T11:45:25Z out:2026-08-06T11:45:26Z reply:2026-08-06T11:04:42Z";

    private static final String EARLIER_STATUSLINE =
        "  call:2026-08-06T10:21:59Z out:2026-08-06T11:04:50Z reply:2026-08-06T11:04:42Z";

    private static void scan(SessionNewActivityStore store, String screenText) {
        new SessionStatuslineReloadScanner().repopulateFromCurrentStatusline(
            store, SESSION_NAME, screenText, NOW_MILLIS, TimeZone.getTimeZone("Asia/Tokyo"));
    }

    @Test
    public void aStatuslineWhoseCallIsNewerThanItsReplyArmsTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, CURRENT_STATUSLINE);
        Assert.assertEquals(Long.valueOf(CALL_MILLIS),
            store.statuslineCallPendingTimeMillis(SESSION_NAME));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void rawKeystrokesAfterTheCallDoNotClearTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, CURRENT_STATUSLINE);
        store.recordUserInput(SESSION_NAME, Instant.parse("2026-08-06T11:50:00Z").toEpochMilli());
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void aGenuineAppReplySentBeforeTheCallDoesNotClearTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordGenuineAppReply(SESSION_NAME, REPLY_MILLIS);
        scan(store, CURRENT_STATUSLINE);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void anEarlierScanOfAnOlderCallDoesNotBlockTheNewerCallFromArmingTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, EARLIER_STATUSLINE);
        scan(store, CURRENT_STATUSLINE);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void scrollbackHoldingAnOlderRenderDoesNotOverrideTheCurrentStatusline() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, EARLIER_STATUSLINE + "\nsome output\n" + CURRENT_STATUSLINE);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void anInPlaceReconnectPurgeKeepsTheCallNewerThanTheReplyAndStaysRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, CURRENT_STATUSLINE);
        store.purgeSessionKeepingTheCallAndReplyTimes(SESSION_NAME);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void aRescanAfterAnInPlaceReconnectPurgeStaysRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, CURRENT_STATUSLINE);
        store.purgeSessionKeepingTheCallAndReplyTimes(SESSION_NAME);
        scan(store, CURRENT_STATUSLINE);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }

    @Test
    public void theOwnerReplyThatArrivesAfterTheCallClearsTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        scan(store, CURRENT_STATUSLINE);
        store.recordGenuineAppReply(SESSION_NAME,
            Instant.parse("2026-08-06T12:07:22Z").toEpochMilli());
        Assert.assertEquals(SessionNewActivityTier.GRAY,
            store.tierFor(SESSION_NAME, Instant.parse("2026-08-06T12:30:00Z").toEpochMilli()));
    }
}
