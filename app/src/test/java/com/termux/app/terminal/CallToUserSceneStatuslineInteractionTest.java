package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class CallToUserSceneStatuslineInteractionTest {

    private static final String SESSION = "worker-session";
    private static final String REASON = "NEEDS APPROVAL TO DEPLOY";

    private static String statuslineWithReplyClock(long replyClockMillis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(replyClockMillis);
        return String.format(Locale.US, "claude  out:%02d:%02d:%02d  reply:%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND), calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    private static void replayStatuslineForVisibleScreen(SessionNewActivityStore store,
                                                         String visibleScreen, long nowMillis,
                                                         TimeZone timeZone) {
        ClaudeStatuslineTimes times =
            ClaudeStatuslineTimes.parse(visibleScreen, nowMillis, timeZone);
        if (times.hasAnyToken()) {
            store.recordStatuslineTimes(SESSION, times.getCallTimeMillis(),
                times.getOutTimeMillis(), times.getReplyTimeMillis());
        }
    }

    @Test
    public void freshCallToUserSceneSurvivesAnOlderStatuslineReplyTokenInTheSameRender() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        long nowMillis = 12L * 60L * 60L * 1000L + 30L * 1000L;
        long replyClockMillis = nowMillis - 2L * 60L * 1000L;

        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = new CallToUserTagController((sessionKey, reason) ->
            store.recordExplicitCall(sessionKey, nowMillis, reason));

        controller.onSessionTextChanged(SESSION,
            "build finished\n<call-to-user>" + REASON + "</call-to-user>");

        replayStatuslineForVisibleScreen(store,
            statuslineWithReplyClock(replyClockMillis, timeZone), nowMillis, timeZone);

        PendingCallToUserFooterDecision decision = PendingCallToUserFooterDecision.resolveAll(
            store.tierFor(SESSION), store.getUnacknowledgedCallReasons(SESSION));

        Assert.assertEquals("the unacknowledged reason recorded by the call-to-user scan must remain "
                + "pending when only an older statusline reply clock token, not a real user reply newer "
                + "than the call, is present", 1, store.getUnacknowledgedCallReasons(SESSION).size());
        Assert.assertTrue("the scene must stay visible after an older statusline reply token is parsed "
            + "in the same render that detected a fresh call-to-user tag", decision.isVisible());
        Assert.assertEquals(REASON, decision.getReportText());
    }

    @Test
    public void statuslineReplyNewerThanTheCallToUserScanStillAcknowledgesTheScene() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        long callScanMillis = 12L * 60L * 60L * 1000L;
        long replyClockMillis = callScanMillis + 2L * 60L * 1000L;
        long nowMillis = replyClockMillis + 60L * 1000L;

        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = new CallToUserTagController((sessionKey, reason) ->
            store.recordExplicitCall(sessionKey, callScanMillis, reason));

        controller.onSessionTextChanged(SESSION,
            "build finished\n<call-to-user>" + REASON + "</call-to-user>");

        replayStatuslineForVisibleScreen(store,
            statuslineWithReplyClock(replyClockMillis, timeZone), nowMillis, timeZone);

        Assert.assertTrue("a statusline reply genuinely newer than the call-to-user scan time must "
            + "acknowledge the pending reason", store.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }
}
