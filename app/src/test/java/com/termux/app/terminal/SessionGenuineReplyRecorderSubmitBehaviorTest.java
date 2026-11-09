package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.termux.terminal.TerminalSession;

import org.junit.Test;

public class SessionGenuineReplyRecorderSubmitBehaviorTest {

    @Test
    public void recordingReplyStoresSessionReplyAndDeletesAnsweredOwnerCalls() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        TerminalSession session = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], 2000, null);
        session.mSessionName = "reply-recorder-session";
        store.recordExplicitCall(session.mSessionName, 1000L, "please answer");
        int[] deleteCallCount = {0};

        boolean recorded = new SessionGenuineReplyRecorder(
            store,
            sessionName -> {
                assertEquals(session.mSessionName, sessionName);
                deleteCallCount[0]++;
            },
            () -> { }
        ).recordReply(session, 2000L, true);

        assertTrue(recorded);
        assertEquals(Long.valueOf(2000L), store.getLastUserInputTimeMillis(session.mSessionName));
        assertEquals(1, deleteCallCount[0]);
        assertTrue(store.getUnacknowledgedCallReasons(session.mSessionName).isEmpty());
    }
}
