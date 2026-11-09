package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;

import com.termux.app.terminal.tts.TtsUtteranceOutcome;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolRuntimeTest {

    @Test
    public void pollsOnMainLooperEveryTwoSecondsAndStopRemovesPolling() {
        List<VoicePatrolCall> calls = new ArrayList<>();
        VoicePatrolController controller = new VoicePatrolController("endpoint",
            () -> new ArrayList<>(calls),
            new VoicePatrolSpeech() {
                @Override
                public void speak(String text, VoicePatrolSpeechOutcome outcome) {
                }

                @Override
                public void stop() {
                }
            },
            new VoicePatrolSessionActions() {
                @Override
                public void showSession(String sessionName) {
                }

                @Override
                public void sendReply(String sessionName, String replyText) {
                }
            },
            (endpointUrl, listener) -> null,
            text -> {
            });
        VoicePatrolRuntime.start(controller);
        assertTrue(VoicePatrolRuntime.isRunning());
        ShadowLooper looper = Shadows.shadowOf(Looper.getMainLooper());

        looper.idleFor(Duration.ofMillis(1999));
        assertEquals(VoicePatrolPhase.WAITING_FOR_CALL, controller.getPhase());
        calls.add(new VoicePatrolCall("alpha", "body"));
        looper.idleFor(Duration.ofMillis(1));
        assertEquals(VoicePatrolPhase.READING_CALL, controller.getPhase());

        VoicePatrolRuntime.stop();
        assertFalse(VoicePatrolRuntime.isRunning());
        assertEquals(VoicePatrolPhase.STOPPED, controller.getPhase());
    }
}
