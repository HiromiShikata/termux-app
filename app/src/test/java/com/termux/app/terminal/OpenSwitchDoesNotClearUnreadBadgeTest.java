package com.termux.app.terminal;

import com.termux.app.terminal.io.SendButtonReplySubmitDecision;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OpenSwitchDoesNotClearUnreadBadgeTest {

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

    private SessionNewActivityStore storeWithPendingRedBadge() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReply = NOW_MILLIS - (2L * ONE_MINUTE_MILLIS);
        long call = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long out = NOW_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes(SESSION_NAME, call, out, staleReply);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
        return store;
    }

    @Test
    public void openingOrDisplayingASessionDoesNotClearItsUnreadBadge() {
        SessionNewActivityStore store = storeWithPendingRedBadge();

        store.recordSeen(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
        Assert.assertNotNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
    }

    @Test
    public void switchingToASessionThatRefreshesItsStatuslineDoesNotClearItsUnreadBadge() {
        SessionNewActivityStore store = storeWithPendingRedBadge();

        long call = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long out = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long staleReply = NOW_MILLIS - (2L * ONE_MINUTE_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS);
        store.recordStatuslineTimes(SESSION_NAME, call, out, staleReply);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
        Assert.assertNotNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
    }

    @Test
    public void aBareSendWithNoOwnerContentDoesNotClearTheUnreadBadge() {
        SessionNewActivityStore store = storeWithPendingRedBadge();

        boolean ownerContentSubmitted = false;
        if (SendButtonReplySubmitDecision.shouldRecordReply(ownerContentSubmitted)) {
            new SessionReplyTimeRecorder(store).recordReplyOnSubmit(newSession(SESSION_NAME), NOW_MILLIS);
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
        Assert.assertNotNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
    }

    @Test
    public void anActualOwnerSubmitClearsTheUnreadBadgeImmediatelyWhileStatuslineReplyIsStale() {
        SessionNewActivityStore store = storeWithPendingRedBadge();

        boolean ownerContentSubmitted = true;
        if (SendButtonReplySubmitDecision.shouldRecordReply(ownerContentSubmitted)) {
            new SessionReplyTimeRecorder(store).recordReplyOnSubmit(newSession(SESSION_NAME), NOW_MILLIS);
        }

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
        Assert.assertFalse(store.hasPendingExplicitCall(SESSION_NAME));
    }
}
