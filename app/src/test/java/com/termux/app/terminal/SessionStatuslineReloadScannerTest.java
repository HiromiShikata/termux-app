package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SessionStatuslineReloadScannerTest {

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

    private static String statusline(long outMillis, long replyMillis) {
        return "claude  out:" + clock(outMillis) + "  reply:" + clock(replyMillis);
    }

    private static String statuslineWithCall(long callMillis, long outMillis, long replyMillis) {
        return "claude  call:" + clock(callMillis) + "  out:" + clock(outMillis)
            + "  reply:" + clock(replyMillis);
    }

    @Test
    public void coldStartRecentReplyRendersRecentTimeNotMoreThanOneDay() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = NOW_MILLIS - 2L * 60L * 1000L;
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put("idle-session", statusline(NOW_MILLIS - 3L * 60L * 1000L, replyMillis));

        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, screens, NOW_MILLIS, UTC);

        Assert.assertEquals(Long.valueOf(replyMillis), store.getStatuslineReplyTimeMillis("idle-session"));
        String text = SessionTimesLine.of(
            store.getStatuslineCallTimeMillis("idle-session"),
            store.getStatuslineOutTimeMillis("idle-session"),
            store.getStatuslineReplyTimeMillis("idle-session"),
            store.getSubagentCount("idle-session"),
            NOW_MILLIS).getText();
        Assert.assertTrue("reply must render the recent time after the on-load scan, got: " + text,
            text.contains("reply: 2m"));
        Assert.assertFalse("reply must not render the more-than-one-day label after the on-load scan, "
            + "got: " + text, text.contains("reply: " + SessionNewActivityStore.MORE_THAN_ONE_DAY_LABEL));
    }

    @Test
    public void coldStartCallNewerThanReplyMarksSessionUnread() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 1L * 60L * 1000L;
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put("calling-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 30L * 1000L, replyMillis));

        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, screens, NOW_MILLIS, UTC);

        Assert.assertTrue("a statusline whose call is newer than its reply must arm the unread "
            + "call-to-user mark on the on-load scan",
            store.hasPendingExplicitCall("calling-session"));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("calling-session"));
    }

    @Test
    public void coldStartReplyCaughtUpToCallDoesNotMarkSessionUnread() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = NOW_MILLIS - 5L * 60L * 1000L;
        long replyMillis = NOW_MILLIS - 1L * 60L * 1000L;
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put("answered-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 30L * 1000L, replyMillis));

        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, screens, NOW_MILLIS, UTC);

        Assert.assertFalse("a statusline whose reply caught up to its call must not arm the unread mark",
            store.hasPendingExplicitCall("answered-session"));
    }

    @Test
    public void coldStartScanRepopulatesEveryProvidedSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = NOW_MILLIS - 4L * 60L * 1000L;
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put("first-session", statusline(NOW_MILLIS - 5L * 60L * 1000L, replyMillis));
        screens.put("second-session", statusline(NOW_MILLIS - 6L * 60L * 1000L, replyMillis));

        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, screens, NOW_MILLIS, UTC);

        Assert.assertEquals(Long.valueOf(replyMillis), store.getStatuslineReplyTimeMillis("first-session"));
        Assert.assertEquals(Long.valueOf(replyMillis), store.getStatuslineReplyTimeMillis("second-session"));
    }

    @Test
    public void sessionWithoutStatuslineTokensIsLeftUntouched() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Map<String, String> screens = new LinkedHashMap<>();
        screens.put("plain-shell-session", "user@device:~$ ls -la");

        new SessionStatuslineReloadScanner()
            .repopulateFromCurrentStatuslines(store, screens, NOW_MILLIS, UTC);

        Assert.assertNull(store.getStatuslineReplyTimeMillis("plain-shell-session"));
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("plain-shell-session"));
    }
}
