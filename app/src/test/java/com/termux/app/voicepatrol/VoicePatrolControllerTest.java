package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeCallSource;
import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeConnection;
import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeConnectionFactory;
import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeLog;
import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeSessions;
import com.termux.app.voicepatrol.VoicePatrolTestDoubles.FakeSpeech;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static com.termux.app.voicepatrol.VoicePatrolTestDoubles.functionCallEvent;
import static com.termux.app.voicepatrol.VoicePatrolTestDoubles.sessionClosedEvent;
import static com.termux.app.voicepatrol.VoicePatrolTestDoubles.sessionStartedEvent;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolControllerTest {

    private static final String ENDPOINT = "https://pc.example/api/live/sessions?k=TOKEN";

    private static final class Harness {
        final FakeCallSource calls = new FakeCallSource();
        final FakeSpeech speech = new FakeSpeech();
        final FakeSessions sessions = new FakeSessions();
        final FakeConnectionFactory connections = new FakeConnectionFactory();
        final FakeLog log = new FakeLog();
        final VoicePatrolController controller = new VoicePatrolController(
            ENDPOINT, calls, speech, sessions, connections, log);

        FakeConnection connection() {
            return connections.last();
        }
    }

    private static VoicePatrolCall call(String sessionName, String body) {
        return new VoicePatrolCall(sessionName, body);
    }

    private static String sentFunctionOutputFor(FakeConnection connection, int index) {
        return connection.sentEvents.get(index);
    }

    @Test
    public void readsTheCallAloudFirstAndOpensTheSessionOnlyAfterReadingEnds() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Please confirm the order."));

        harness.controller.start();

        assertEquals(VoicePatrolPhase.READING_CALL, harness.controller.getPhase());
        assertEquals(Arrays.asList("Please confirm the order."), harness.speech.spokenTexts);
        assertTrue(harness.connections.opened.isEmpty());
        assertEquals(Arrays.asList("alpha"), harness.sessions.shownSessions);

        harness.speech.finishLastUtterance();

        assertEquals(1, harness.connections.opened.size());
        assertEquals(ENDPOINT, harness.connection().endpointUrl);
        assertEquals(VoicePatrolPhase.CONNECTING, harness.controller.getPhase());
    }

    @Test
    public void microphoneIsDisabledWhileTtsSpeaksOverAnOpenSessionAndEnabledAfterwards() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "First."), call("beta", "Second."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        assertEquals(Arrays.asList(true), harness.connection().microphoneStates);

        harness.connection().listener.onEventReceived(functionCallEvent("call_m", "session_skip", "{}"));
        assertEquals(Arrays.asList(true, false), harness.connection().microphoneStates);

        harness.speech.finishLastUtterance();
        assertEquals(Arrays.asList(true, false, true), harness.connection().microphoneStates);
        assertEquals(VoicePatrolPhase.CONVERSING, harness.controller.getPhase());
    }

    @Test
    public void sendsSessionNameAndBodyAsNonSpokenContextAfterSessionStarts() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body text for the owner."));

        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());

        String contextEvents = String.join("\n", harness.connection().sentEvents);
        assertTrue(contextEvents, contextEvents.contains("\"type\":\"session.thinking.append\""));
        assertTrue(contextEvents, contextEvents.contains("\"delegation_id\":null"));
        assertTrue(contextEvents, contextEvents.contains("alpha"));
        assertTrue(contextEvents, contextEvents.contains("Body text for the owner."));
        assertFalse(contextEvents, contextEvents.contains("session.commentary.append"));
    }

    @Test
    public void sessionReplySendSendsReplyTextThroughSessionActionsAndReturnsOutputThenContinues() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "First call."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.connection().listener.onEventReceived(
            functionCallEvent("call_1", "session_reply_send", "\"{\\\"replyText\\\":\\\"yes\\\"}\""));

        assertEquals(Arrays.asList("alpha|yes"), harness.sessions.repliesSessionAndText);
        assertTrue(harness.connection().sentEvents.size() >= 2);
        assertTrue(harness.connection().sentEvents.get(0).contains("\"type\":\"response.item.create\""));
        assertTrue(harness.connection().sentEvents.get(0).contains("\"call_id\":\"call_1\""));
        assertTrue(harness.connection().sentEvents.get(0).contains("\"type\":\"function_call_output\""));
        assertTrue(harness.connection().sentEvents.get(1).contains("\"type\":\"response.create\""));
    }

    @Test
    public void afterReplyTheAppMovesToTheNextCallingSessionInBellOrder() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "First."), call("beta", "Second."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());

        harness.connection().listener.onEventReceived(
            functionCallEvent("call_1", "session_reply_send", "\"{\\\"replyText\\\":\\\"ok\\\"}\""));

        assertEquals(VoicePatrolPhase.READING_CALL, harness.controller.getPhase());
        assertEquals("Second.", harness.speech.spokenTexts.get(harness.speech.spokenTexts.size() - 1));
        assertEquals("beta", harness.sessions.shownSessions.get(harness.sessions.shownSessions.size() - 1));
        assertEquals(1, harness.connections.opened.size());
    }

    @Test
    public void sessionSkipMovesToNextCallWithoutSendingAReply() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "First."), call("beta", "Second."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.connection().listener.onEventReceived(functionCallEvent("call_2", "session_skip", "{}"));

        assertTrue(harness.sessions.repliesSessionAndText.isEmpty());
        assertEquals("beta", harness.sessions.shownSessions.get(harness.sessions.shownSessions.size() - 1));
        assertTrue(harness.connection().sentEvents.get(0).contains("\"call_id\":\"call_2\""));
        assertTrue(harness.connection().sentEvents.get(1).contains("\"type\":\"response.create\""));
    }

    @Test
    public void ownerCallRepeatReadsTheCurrentBodyAgainWithoutAddingContext() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Repeat me."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.connection().listener.onEventReceived(functionCallEvent("call_3", "owner_call_repeat", "{}"));

        assertEquals(VoicePatrolPhase.READING_CALL, harness.controller.getPhase());
        assertEquals("Repeat me.", harness.speech.spokenTexts.get(harness.speech.spokenTexts.size() - 1));
        assertEquals(2, harness.connection().sentEvents.size());
        harness.speech.finishLastUtterance();
        assertEquals(VoicePatrolPhase.CONVERSING, harness.controller.getPhase());
        assertEquals(2, harness.connection().sentEvents.size());
        assertTrue(harness.connections.opened.size() == 1);
    }

    @Test
    public void closesTheSessionWhenNoCallRemainsAndStaysSilentUntilCloseIsConfirmed() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Only call."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.calls.calls = Arrays.asList();
        harness.connection().listener.onEventReceived(
            functionCallEvent("call_4", "session_reply_send", "\"{\\\"replyText\\\":\\\"done\\\"}\""));

        assertEquals(VoicePatrolPhase.WAITING_FOR_CALL, harness.controller.getPhase());
        assertTrue(harness.connection().sentEvents.get(2).contains("\"type\":\"session.close\""));
        assertEquals(0, harness.connection().closeCount);

        harness.connection().listener.onEventReceived(sessionClosedEvent());

        assertEquals(1, harness.connection().closeCount);
    }

    @Test
    public void reopensTheSessionWhenANewCallArrivesAfterClose() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Only call."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.calls.calls = Arrays.asList();
        harness.connection().listener.onEventReceived(
            functionCallEvent("call_5", "session_reply_send", "\"{\\\"replyText\\\":\\\"done\\\"}\""));
        harness.connection().listener.onEventReceived(sessionClosedEvent());

        harness.calls.calls = Arrays.asList(call("gamma", "New call."));
        harness.controller.onCallingSessionsChanged();

        assertEquals(VoicePatrolPhase.READING_CALL, harness.controller.getPhase());
        assertEquals("New call.", harness.speech.spokenTexts.get(harness.speech.spokenTexts.size() - 1));
        harness.speech.finishLastUtterance();
        assertEquals(2, harness.connections.opened.size());
        assertEquals(VoicePatrolPhase.CONNECTING, harness.controller.getPhase());
    }

    @Test
    public void connectFailureSpeaksJapaneseErrorAndStaysWaitingWithoutRetryingTheSameCall() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();

        harness.connection().listener.onConnectFailed("openai_api_key_missing");

        assertEquals(VoicePatrolPhase.WAITING_FOR_CALL, harness.controller.getPhase());
        String lastSpoken = harness.speech.spokenTexts.get(harness.speech.spokenTexts.size() - 1);
        assertTrue(lastSpoken, lastSpoken.startsWith("音声の接続を始められませんでした"));
        assertTrue(lastSpoken, lastSpoken.contains("openai_api_key_missing"));

        harness.controller.onCallingSessionsChanged();

        assertEquals(1, harness.connections.opened.size());
        assertEquals(VoicePatrolPhase.WAITING_FOR_CALL, harness.controller.getPhase());
    }

    @Test
    public void connectFailureIsLoggedNotSwallowed() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();

        harness.connection().listener.onConnectFailed("openai_live_session_failed");

        assertEquals(1, harness.log.warnings.size());
        assertTrue(harness.log.warnings.get(0).contains("openai_live_session_failed"));
    }

    @Test
    public void invalidReplyArgumentsReturnErrorOutputAndStillContinueTheBackend() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.connection().listener.onEventReceived(
            functionCallEvent("call_6", "session_reply_send", "{}"));

        assertTrue(harness.sessions.repliesSessionAndText.isEmpty());
        assertTrue(harness.connection().sentEvents.get(0).contains("\"call_id\":\"call_6\""));
        assertTrue(harness.connection().sentEvents.get(0).contains("error"));
        assertTrue(harness.connection().sentEvents.get(1).contains("\"type\":\"response.create\""));
        assertFalse(harness.log.warnings.isEmpty());
    }

    @Test
    public void unknownToolReturnsErrorOutputAndIsLogged() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());
        harness.connection().sentEvents.clear();

        harness.connection().listener.onEventReceived(functionCallEvent("call_7", "unknown_tool", "{}"));

        assertTrue(harness.connection().sentEvents.get(0).contains("\"call_id\":\"call_7\""));
        assertTrue(harness.connection().sentEvents.get(0).contains("error"));
        assertEquals(1, harness.log.warnings.size());
    }

    @Test
    public void unparseableServerEventIsLoggedAndIgnored() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();

        harness.connection().listener.onEventReceived("not json");

        assertEquals(1, harness.log.warnings.size());
        assertEquals(VoicePatrolPhase.CONNECTING, harness.controller.getPhase());
    }

    @Test
    public void stopClosesConnectionAndSilencesSpeech() {
        Harness harness = new Harness();
        harness.calls.calls = Arrays.asList(call("alpha", "Body."));
        harness.controller.start();
        harness.speech.finishLastUtterance();
        harness.connection().listener.onEventReceived(sessionStartedEvent());

        harness.controller.stop();

        assertEquals(VoicePatrolPhase.STOPPED, harness.controller.getPhase());
        assertEquals(1, harness.connection().closeCount);
        assertTrue(harness.speech.events.contains("speech-stop"));
    }
}
