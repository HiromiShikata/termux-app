package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TermuxVoicePatrolActivityBridgeTest {
    @Test
    public void callingSessionsAreSelectedInBellOrder() {
        FakeSessions sessions = new FakeSessions();
        sessions.names = Arrays.asList("alpha", "beta", "gamma");
        sessions.order = Arrays.asList(2, 0, 1);
        sessions.pending = new HashSet<>(Arrays.asList("alpha", "gamma"));
        sessions.bodies.put("alpha", "first");
        sessions.bodies.put("gamma", "third");
        assertEquals(Arrays.asList(new VoicePatrolCall("gamma", "third"), new VoicePatrolCall("alpha", "first")),
            new TermuxVoicePatrolActivityBridge(sessions).callingSessionsInBellOrder());
    }

    @Test
    public void showSessionSwitchesOnlyWhenActivityIsVisible() {
        FakeSessions sessions = new FakeSessions();
        sessions.names = Arrays.asList("alpha", "beta");
        TermuxVoicePatrolActivityBridge bridge = new TermuxVoicePatrolActivityBridge(sessions);
        bridge.showSession("beta");
        assertEquals(-1, sessions.switchedIndex);
        sessions.visible = true;
        bridge.showSession("beta");
        assertEquals(1, sessions.switchedIndex);
    }

    @Test
    public void sendReplySwitchesThenSubmitsExactTextAndReturnsResult() {
        FakeSessions sessions = new FakeSessions();
        sessions.names = Arrays.asList("alpha", "beta");
        sessions.submitResult = true;
        assertTrue(new TermuxVoicePatrolActivityBridge(sessions).sendReply("beta", "exact reply"));
        assertEquals(1, sessions.switchedIndex);
        assertEquals("exact reply", sessions.submittedText);
    }

    @Test
    public void sendReplyReturnsFalseForAbsentSessionWithoutSubmitting() {
        FakeSessions sessions = new FakeSessions();
        sessions.names = Arrays.asList("alpha");
        assertFalse(new TermuxVoicePatrolActivityBridge(sessions).sendReply("beta", "reply"));
        assertEquals(-1, sessions.switchedIndex);
        assertEquals(null, sessions.submittedText);
    }

    private static final class FakeSessions implements VoicePatrolTerminalSessions {
        List<String> names;
        List<Integer> order = Arrays.asList(0);
        Set<String> pending = new HashSet<>();
        Map<String, String> bodies = new HashMap<>();
        boolean visible;
        boolean submitResult;
        int switchedIndex = -1;
        String submittedText;
        public List<String> sessionNames() { return names; }
        public List<Integer> orderedSessionIndexes() { return order; }
        public Set<String> pendingCallSessionNames() { return pending; }
        public Map<String, String> unansweredCallBodiesBySessionName() { return bodies; }
        public boolean isActivityVisible() { return visible; }
        public void switchToSessionAtIndex(int index) { switchedIndex = index; }
        public boolean submitVoiceReplyToCurrentSession(String text) { submittedText = text; return submitResult; }
    }
}
