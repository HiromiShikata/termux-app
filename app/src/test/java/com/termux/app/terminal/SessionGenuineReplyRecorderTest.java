package com.termux.app.terminal;

import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionGenuineReplyRecorderTest {

    private static final String SESSION_NAME = "worker";
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

    private static SessionGenuineReplyRecorder recorderWith(
            SessionNewActivityStore store,
            List<String> deletedSessionNames,
            int[] overlayUpdateCount) {
        return new SessionGenuineReplyRecorder(
            store,
            deletedSessionNames::add,
            () -> overlayUpdateCount[0]++);
    }

    @Test
    public void recordReplyStoresTheSubmitTimeInTheStore() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        boolean recorded = recorderWith(store, new ArrayList<>(), new int[]{0})
            .recordReply(newSession(SESSION_NAME), NOW_MILLIS, false);

        Assert.assertTrue(recorded);
        Assert.assertEquals(Long.valueOf(NOW_MILLIS), store.getLastUserInputTimeMillis(SESSION_NAME));
    }

    @Test
    public void recordReplyDeletesTheAnsweredOwnerCallsForTheSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        List<String> deletedSessionNames = new ArrayList<>();

        recorderWith(store, deletedSessionNames, new int[]{0})
            .recordReply(newSession(SESSION_NAME), NOW_MILLIS, false);

        Assert.assertEquals(1, deletedSessionNames.size());
        Assert.assertEquals(SESSION_NAME, deletedSessionNames.get(0));
    }

    @Test
    public void recordReplyUpdatesTheSessionNameOverlayWhenSessionIsTheCurrent() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        int[] overlayUpdateCount = {0};

        recorderWith(store, new ArrayList<>(), overlayUpdateCount)
            .recordReply(newSession(SESSION_NAME), NOW_MILLIS, true);

        Assert.assertEquals(1, overlayUpdateCount[0]);
    }

    @Test
    public void recordReplyDoesNotUpdateSessionNameOverlayWhenSessionIsNotTheCurrent() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        int[] overlayUpdateCount = {0};

        recorderWith(store, new ArrayList<>(), overlayUpdateCount)
            .recordReply(newSession(SESSION_NAME), NOW_MILLIS, false);

        Assert.assertEquals(0, overlayUpdateCount[0]);
    }

    @Test
    public void recordReplyReturnsFalseForNullSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        List<String> deletedSessionNames = new ArrayList<>();
        int[] overlayUpdateCount = {0};

        boolean recorded = recorderWith(store, deletedSessionNames, overlayUpdateCount)
            .recordReply(null, NOW_MILLIS, false);

        Assert.assertFalse(recorded);
        Assert.assertTrue(deletedSessionNames.isEmpty());
        Assert.assertEquals(0, overlayUpdateCount[0]);
    }

    @Test
    public void recordReplyReturnsFalseForUnnamedSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        List<String> deletedSessionNames = new ArrayList<>();
        int[] overlayUpdateCount = {0};

        boolean recorded = recorderWith(store, deletedSessionNames, overlayUpdateCount)
            .recordReply(newSession(null), NOW_MILLIS, false);

        Assert.assertFalse(recorded);
        Assert.assertTrue(deletedSessionNames.isEmpty());
        Assert.assertEquals(0, overlayUpdateCount[0]);
    }
}
