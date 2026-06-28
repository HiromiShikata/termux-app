package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AllSessionsStatuslineParserTest {

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

    private static String statusline(long outMillis, long replyMillis) {
        return "claude  out:" + clock(outMillis) + "  reply:" + clock(replyMillis);
    }

    private static AllSessionsStatuslineParser.SessionScreenText screenText(String sessionName,
                                                                           String text) {
        return new AllSessionsStatuslineParser.SessionScreenText(sessionName, text);
    }

    @Test
    public void parsesEachSessionTokensIntoUpdates() {
        long replyMillis = NOW_MILLIS - 4L * 60L * 1000L;
        long outMillis = NOW_MILLIS - 5L * 60L * 1000L;
        List<AllSessionsStatuslineParser.SessionScreenText> inputs = new ArrayList<>();
        inputs.add(screenText("first-session", statusline(outMillis, replyMillis)));
        inputs.add(screenText("second-session", statusline(outMillis, replyMillis)));

        List<ParsedStatuslineUpdate> updates =
            new AllSessionsStatuslineParser().parse(inputs, NOW_MILLIS, UTC);

        Assert.assertEquals(2, updates.size());
        Assert.assertEquals("first-session", updates.get(0).getSessionName());
        Assert.assertEquals(Long.valueOf(outMillis), updates.get(0).getOutTimeMillis());
        Assert.assertEquals(Long.valueOf(replyMillis), updates.get(0).getReplyTimeMillis());
    }

    @Test
    public void skipsSessionsWithoutAnyStatuslineToken() {
        List<AllSessionsStatuslineParser.SessionScreenText> inputs = new ArrayList<>();
        inputs.add(screenText("plain-shell-session", "user@device:~$ ls -la"));

        List<ParsedStatuslineUpdate> updates =
            new AllSessionsStatuslineParser().parse(inputs, NOW_MILLIS, UTC);

        Assert.assertTrue(updates.isEmpty());
    }

    @Test
    public void parsedUpdateAppliedToStoreArmsUnreadMarkForCallNewerThanReply() {
        long replyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 1L * 60L * 1000L;
        List<AllSessionsStatuslineParser.SessionScreenText> inputs = new ArrayList<>();
        inputs.add(screenText("calling-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 30L * 1000L, replyMillis)));

        List<ParsedStatuslineUpdate> updates =
            new AllSessionsStatuslineParser().parse(inputs, NOW_MILLIS, UTC);
        SessionNewActivityStore store = new SessionNewActivityStore();
        for (ParsedStatuslineUpdate update : updates) {
            update.applyTo(store);
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("calling-session"));
        Assert.assertTrue(store.hasPendingExplicitCall("calling-session"));
    }

    @Test
    public void unchangedSessionIsSkippedByGateBeforeReachingTheParser() {
        AllSessionsStatuslineScanGate gate = new AllSessionsStatuslineScanGate();
        long replyMillis = NOW_MILLIS - 10L * 60L * 1000L;
        long callMillis = NOW_MILLIS - 1L * 60L * 1000L;
        String callingText = statuslineWithCall(callMillis, NOW_MILLIS - 30L * 1000L, replyMillis);

        List<AllSessionsStatuslineParser.SessionScreenText> firstPassInputs = new ArrayList<>();
        if (gate.shouldScan("calling-session", 100L)) {
            firstPassInputs.add(screenText("calling-session", callingText));
        }
        Assert.assertEquals(1, firstPassInputs.size());

        List<AllSessionsStatuslineParser.SessionScreenText> secondPassInputs = new ArrayList<>();
        if (gate.shouldScan("calling-session", 100L)) {
            secondPassInputs.add(screenText("calling-session", callingText));
        }
        Assert.assertTrue("an unchanged session must be skipped before the parser",
            secondPassInputs.isEmpty());

        SessionNewActivityStore store = new SessionNewActivityStore();
        for (ParsedStatuslineUpdate update :
                new AllSessionsStatuslineParser().parse(firstPassInputs, NOW_MILLIS, UTC)) {
            update.applyTo(store);
        }

        Assert.assertNotNull("the gate-passing session must be armed RED via its statusline call signal",
            store.statuslineCallPendingTimeMillis("calling-session"));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("calling-session"));
    }

    @Test
    public void answeredSessionAppliedToStoreDoesNotArmRedTier() {
        long callMillis = NOW_MILLIS - 5L * 60L * 1000L;
        long replyMillis = NOW_MILLIS - 1L * 60L * 1000L;
        List<AllSessionsStatuslineParser.SessionScreenText> inputs = new ArrayList<>();
        inputs.add(screenText("answered-session",
            statuslineWithCall(callMillis, NOW_MILLIS - 30L * 1000L, replyMillis)));

        SessionNewActivityStore store = new SessionNewActivityStore();
        for (ParsedStatuslineUpdate update :
                new AllSessionsStatuslineParser().parse(inputs, NOW_MILLIS, UTC)) {
            update.applyTo(store);
        }

        Assert.assertFalse(store.hasPendingExplicitCall("answered-session"));
        Assert.assertNull(store.statuslineCallPendingTimeMillis("answered-session"));
    }

    @Test
    public void pendingCallSessionCountReflectsSessionsWithUnacknowledgedReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("first-calling-session", NOW_MILLIS, "needs input");
        store.recordExplicitCall("second-calling-session", NOW_MILLIS, "needs input too");
        store.recordExplicitCall("answered-session", NOW_MILLIS, "done");
        store.recordUserInput("answered-session", NOW_MILLIS + 1000L);

        Assert.assertEquals(2, store.pendingCallToUserSessionCount());
        Assert.assertEquals("2/3 calls",
            PendingCallNotificationText.fractionSuffix(store.pendingCallToUserSessionCount(), 3));
    }
}
