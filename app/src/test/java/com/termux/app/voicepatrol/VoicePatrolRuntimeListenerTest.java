package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolRuntimeListenerTest {

    private static VoicePatrolController controller() {
        return new VoicePatrolController("endpoint",
            () -> new ArrayList<VoicePatrolCall>(),
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
                public boolean sendReply(String sessionName, String replyText) {
                    return true;
                }
            },
            (endpointUrl, listener) -> null,
            message -> {
            });
    }

    @Test
    public void listenersAreToldWhenThePatrolStartsAndWhenItStops() {
        List<Boolean> seen = new ArrayList<>();
        VoicePatrolRuntime.Listener listener = seen::add;
        VoicePatrolRuntime.addListener(listener);
        try {
            VoicePatrolRuntime.start(controller());
            VoicePatrolRuntime.stop();

            assertEquals(Arrays.asList(true, false), seen);
        } finally {
            VoicePatrolRuntime.removeListener(listener);
            VoicePatrolRuntime.stop();
        }
    }

    @Test
    public void stoppingAPatrolThatIsNotRunningTellsNoOne() {
        List<Boolean> seen = new ArrayList<>();
        VoicePatrolRuntime.Listener listener = seen::add;
        VoicePatrolRuntime.addListener(listener);
        try {
            VoicePatrolRuntime.stop();

            assertEquals(new ArrayList<Boolean>(), seen);
        } finally {
            VoicePatrolRuntime.removeListener(listener);
        }
    }

    @Test
    public void removedListenerIsNoLongerTold() {
        List<Boolean> seen = new ArrayList<>();
        VoicePatrolRuntime.Listener listener = seen::add;
        VoicePatrolRuntime.addListener(listener);
        VoicePatrolRuntime.removeListener(listener);
        try {
            VoicePatrolRuntime.start(controller());
            VoicePatrolRuntime.stop();

            assertEquals(new ArrayList<Boolean>(), seen);
        } finally {
            VoicePatrolRuntime.stop();
        }
    }
}
