package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.terminal.tts.TtsUtteranceOutcome;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolAdaptersTest {

    static final class Bridge implements VoicePatrolActivityBridge {
        final List<VoicePatrolCall> calls = Arrays.asList(new VoicePatrolCall("s", "b"));
        final List<String> shown = new ArrayList<>();
        boolean replyAccepted = true;

        @Override
        public List<VoicePatrolCall> callingSessionsInBellOrder() {
            return calls;
        }

        @Override
        public void showSession(String sessionName) {
            shown.add(sessionName);
        }

        @Override
        public boolean sendReply(String sessionName, String replyText) {
            return replyAccepted;
        }
    }

    static final class LogSink implements VoicePatrolLog {
        final List<String> warnings = new ArrayList<>();

        @Override
        public void warn(String message) {
            warnings.add(message);
        }
    }

    static final class NoopSynthesizer implements TtsManager.SpeechSynthesizer {
        @Override
        public void initialize(Runnable onReady) {
        }

        @Override
        public void speak(String text, int queueMode, String utteranceId) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void shutdown() {
        }
    }

    static final class FakeTts extends TtsManager {
        String text;
        TtsUtteranceOutcome outcome;
        boolean stopped;

        FakeTts() {
            super(new NoopSynthesizer());
        }

        @Override
        public void speak(String value, TtsUtteranceOutcome valueOutcome) {
            text = value;
            outcome = valueOutcome;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

    @Test
    public void portsDelegateToBridgeAndWarnWhenReplyIsRejected() {
        Bridge bridge = new Bridge();
        LogSink log = new LogSink();
        TermuxVoicePatrolPorts ports = new TermuxVoicePatrolPorts(bridge, log);

        assertEquals(bridge.calls, ports.callingSessionsInBellOrder());
        ports.showSession("s");
        assertEquals(Arrays.asList("s"), bridge.shown);
        bridge.replyAccepted = false;
        ports.sendReply("s", "r");
        assertEquals(1, log.warnings.size());
    }

    @Test
    public void logAdapterWritesWarningWithVoicePatrolTag() {
        new AndroidLogVoicePatrolLog().warn("warning");
        ShadowLog.LogItem item = ShadowLog.getLogs().get(ShadowLog.getLogs().size() - 1);

        assertEquals("warning", item.msg);
        assertEquals(Log.WARN, item.type);
        assertEquals("TermuxVoicePatrol", item.tag);
    }

    @Test
    public void ttsAdapterDelegatesSpeechAndStop() {
        FakeTts tts = new FakeTts();
        TtsVoicePatrolSpeech speech = new TtsVoicePatrolSpeech(tts);
        VoicePatrolSpeechOutcome outcome = new VoicePatrolSpeechOutcome() {
            @Override
            public void onSpoken() {
            }

            @Override
            public void onFailed(String reason) {
            }
        };

        speech.speak("x", outcome);
        speech.stop();

        assertEquals("x", tts.text);
        assertTrue(tts.outcome != null);
        assertTrue(tts.stopped);
        assertFalse(outcome == null);
        assertSame(outcome, outcome);
    }
}
