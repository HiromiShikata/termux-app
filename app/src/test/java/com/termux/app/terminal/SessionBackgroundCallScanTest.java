package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SessionBackgroundCallScanTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final long NOW_MILLIS =
        12L * 60L * 60L * 1000L + 30L * 60L * 1000L + 45L * 1000L;

    private static String clock(long timeMillis) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTimeInMillis(timeMillis);
        return String.format(Locale.US, "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND));
    }

    private static String statuslineWithCall(long callMillis, long outMillis, long replyMillis) {
        return "claude  call:" + clock(callMillis) + "  out:" + clock(outMillis)
            + "  reply:" + clock(replyMillis);
    }

    private static String statuslineRepliedCaughtUp(long outMillis, long replyMillis) {
        return "claude  out:" + clock(outMillis) + "  reply:" + clock(replyMillis);
    }

    private void scan(SessionNewActivityStore store, Map<String, String> sessionBuffersByName,
                      long nowMillis) {
        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, sessionBuffersByName, nowMillis, UTC);
    }

    @Test
    public void periodicSweepArmsRedOnlyForBackgroundSessionWithUnrepliedCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReplyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 1L * 60L * 1000L;
        long freshReplyMillis = NOW_MILLIS - 30L * 1000L;
        Map<String, String> sessionBuffersByName = new LinkedHashMap<>();
        sessionBuffersByName.put("unreplied-call-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 45L * 1000L, staleReplyMillis));
        sessionBuffersByName.put("replied-session",
            statuslineRepliedCaughtUp(NOW_MILLIS - 2L * 60L * 1000L, freshReplyMillis));

        scan(store, sessionBuffersByName, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("unreplied-call-session"));
        Assert.assertTrue(store.hasPendingExplicitCall("unreplied-call-session"));
        Assert.assertNotEquals("a session whose reply caught up to its call must not be RED",
            SessionNewActivityTier.RED, store.tierFor("replied-session"));
        Assert.assertFalse(store.hasPendingExplicitCall("replied-session"));
    }

    @Test
    public void periodicSweepKeepsRedUntilReplyTokenCatchesUp() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReplyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 5L * 60L * 1000L;
        Map<String, String> firstSweep = new LinkedHashMap<>();
        firstSweep.put("background-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 5L * 60L * 1000L, staleReplyMillis));

        scan(store, firstSweep, NOW_MILLIS);

        Assert.assertEquals("the first sweep arms RED for the un-replied call",
            SessionNewActivityTier.RED, store.tierFor("background-session"));

        long laterNowMillis = NOW_MILLIS + 1L * 60L * 1000L;
        Map<String, String> secondSweepStillUnreplied = new LinkedHashMap<>();
        secondSweepStillUnreplied.put("background-session",
            statuslineWithCall(callMillis, laterNowMillis - 5L * 60L * 1000L, staleReplyMillis));

        scan(store, secondSweepStillUnreplied, laterNowMillis);

        Assert.assertEquals("a later sweep with the reply still behind the call keeps RED armed",
            SessionNewActivityTier.RED, store.tierFor("background-session"));

        long replyCaughtUpMillis = callMillis + 30L * 1000L;
        Map<String, String> thirdSweepReplied = new LinkedHashMap<>();
        thirdSweepReplied.put("background-session",
            statuslineWithCall(callMillis, laterNowMillis - 4L * 60L * 1000L, replyCaughtUpMillis));

        scan(store, thirdSweepReplied, laterNowMillis);

        Assert.assertFalse("once the reply token catches up to the call the sweep clears RED",
            store.hasPendingExplicitCall("background-session"));
    }

    @Test
    public void repeatedSweepWithoutChangeDoesNotReArmAfterAppSideReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReplyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 2L * 60L * 1000L;
        String unrepliedBuffer =
            statuslineWithCall(callMillis, NOW_MILLIS - 90L * 1000L, staleReplyMillis);
        Map<String, String> sweep = new LinkedHashMap<>();
        sweep.put("background-session", unrepliedBuffer);

        scan(store, sweep, NOW_MILLIS);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("background-session"));

        store.recordUserInput("background-session", NOW_MILLIS);
        Assert.assertFalse("owner input clears the pending call",
            store.hasPendingExplicitCall("background-session"));

        scan(store, sweep, NOW_MILLIS + 5L * 60L * 1000L);

        Assert.assertFalse("a later sweep of the same unchanged buffer must not re-arm RED after the "
            + "owner already replied in-app", store.hasPendingExplicitCall("background-session"));
    }

    @Test
    public void periodicSweepArmsRedForBackgroundSessionThatHadNoPriorState() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReplyMillis = NOW_MILLIS - 30L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 3L * 60L * 1000L;
        Map<String, String> sweep = new LinkedHashMap<>();
        sweep.put("never-opened-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 2L * 60L * 1000L, staleReplyMillis));

        Assert.assertEquals("before any sweep an unopened session has no indicator",
            SessionNewActivityTier.NONE, store.tierFor("never-opened-session"));

        scan(store, sweep, NOW_MILLIS);

        Assert.assertEquals("the periodic sweep arms RED for a session the owner never opened",
            SessionNewActivityTier.RED, store.tierFor("never-opened-session"));
    }

    @Test
    public void periodicSweepReArmsRedWhenANewCallTimeArrivesAfterAReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long firstCallMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long firstReplyMillis = NOW_MILLIS - 9L * 60L * 1000L;
        Map<String, String> repliedSweep = new LinkedHashMap<>();
        repliedSweep.put("background-session",
            statuslineWithCall(firstCallMillis, NOW_MILLIS - 9L * 60L * 1000L, firstReplyMillis));

        scan(store, repliedSweep, NOW_MILLIS);

        Assert.assertNotEquals("a call whose reply already caught up is not RED",
            SessionNewActivityTier.RED, store.tierFor("background-session"));

        long secondCallMillis = NOW_MILLIS - 1L * 60L * 1000L;
        Map<String, String> newCallSweep = new LinkedHashMap<>();
        newCallSweep.put("background-session",
            statuslineWithCall(secondCallMillis, NOW_MILLIS - 30L * 1000L, firstReplyMillis));

        scan(store, newCallSweep, NOW_MILLIS);

        Assert.assertEquals("a newer call time than the last reply re-arms RED on the next sweep",
            SessionNewActivityTier.RED, store.tierFor("background-session"));
    }

    @Test
    public void periodicSweepDoesNotReArmWhenTheCallTimeIsUnchanged() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = NOW_MILLIS - 2L * 60L * 1000L;
        long staleReplyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        Map<String, String> sweep = new LinkedHashMap<>();
        sweep.put("background-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 90L * 1000L, staleReplyMillis));

        scan(store, sweep, NOW_MILLIS);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("background-session"));

        store.recordUserInput("background-session", NOW_MILLIS);
        Assert.assertFalse("owner input clears the pending call",
            store.hasPendingExplicitCall("background-session"));

        scan(store, sweep, NOW_MILLIS + 6L * 60L * 1000L);

        Assert.assertFalse("an unchanged call time must not re-arm RED after the owner replied",
            store.hasPendingExplicitCall("background-session"));
    }
}
