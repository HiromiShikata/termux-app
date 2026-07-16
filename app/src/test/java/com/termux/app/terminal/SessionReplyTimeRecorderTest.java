package com.termux.app.terminal;

import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionReplyTimeRecorderTest {

    private static final String SESSION_NAME = "worker";
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
    private static final long NOW_MILLIS = 1_000_000_000L;

    @Before
    public void setUp() {
        TermuxShellManager.init(RuntimeEnvironment.getApplication());
    }

    private static TerminalSession newSession(String sessionName) {
        TerminalSession session = new TerminalSession(
            "/system/bin/sh", "/", new String[0], new String[0], 100, null);
        session.mSessionName = sessionName;
        return session;
    }

    @Test
    public void recordReplyOnSubmitStoresTheSubmitTimeAsTheOwnerReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionReplyTimeRecorder recorder = new SessionReplyTimeRecorder(store);

        boolean recorded = recorder.recordReplyOnSubmit(newSession(SESSION_NAME), NOW_MILLIS);

        Assert.assertTrue(recorded);
        Assert.assertEquals(Long.valueOf(NOW_MILLIS),
            store.getLastUserInputTimeMillis(SESSION_NAME));
        Assert.assertEquals(Long.valueOf(NOW_MILLIS),
            store.effectiveReplyTimeMillis(SESSION_NAME));
    }

    @Test
    public void recordReplyOnSubmitClearsTheRedBadgeImmediatelyWithoutWaitingForTheAgent() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReply = NOW_MILLIS - (2L * ONE_MINUTE_MILLIS);
        long call = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long out = NOW_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes(SESSION_NAME, call, out, staleReply);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));

        boolean recorded = new SessionReplyTimeRecorder(store)
            .recordReplyOnSubmit(newSession(SESSION_NAME), NOW_MILLIS);

        Assert.assertTrue(recorded);
        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
        Assert.assertFalse(store.hasPendingExplicitCall(SESSION_NAME));
    }

    @Test
    public void onSubmitReplyTimeIsPreferredWhenNewerThanTheScrapedStatuslineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long scrapedStatuslineReply = NOW_MILLIS - (5L * ONE_MINUTE_MILLIS);
        store.recordStatuslineTimes(SESSION_NAME, null, null, scrapedStatuslineReply);

        new SessionReplyTimeRecorder(store)
            .recordReplyOnSubmit(newSession(SESSION_NAME), NOW_MILLIS);

        Assert.assertEquals(Long.valueOf(NOW_MILLIS),
            store.effectiveReplyTimeMillis(SESSION_NAME));
    }

    @Test
    public void scrapedStatuslineReplyIsPreferredWhenNewerThanAnOlderOnSubmitTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long olderSubmit = NOW_MILLIS - (5L * ONE_MINUTE_MILLIS);
        new SessionReplyTimeRecorder(store)
            .recordReplyOnSubmit(newSession(SESSION_NAME), olderSubmit);

        store.recordStatuslineTimes(SESSION_NAME, null, null, NOW_MILLIS);

        Assert.assertEquals(Long.valueOf(NOW_MILLIS),
            store.effectiveReplyTimeMillis(SESSION_NAME));
    }

    @Test
    public void recordReplyOnSubmitIsANoOpForNullSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionReplyTimeRecorder recorder = new SessionReplyTimeRecorder(store);

        boolean recorded = recorder.recordReplyOnSubmit(null, NOW_MILLIS);

        Assert.assertFalse(recorded);
        Assert.assertNull(store.getLastUserInputTimeMillis(SESSION_NAME));
    }

    @Test
    public void recordReplyOnSubmitIsANoOpForUnnamedSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionReplyTimeRecorder recorder = new SessionReplyTimeRecorder(store);

        boolean recorded = recorder.recordReplyOnSubmit(newSession(null), NOW_MILLIS);

        Assert.assertFalse(recorded);
    }
}
