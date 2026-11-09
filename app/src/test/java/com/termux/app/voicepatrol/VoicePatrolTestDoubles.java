package com.termux.app.voicepatrol;

import java.util.ArrayList;
import java.util.List;

final class VoicePatrolTestDoubles {

    private VoicePatrolTestDoubles() {
    }

    static final class FakeCallSource implements VoicePatrolCallSource {
        List<VoicePatrolCall> calls = new ArrayList<>();

        @Override
        public List<VoicePatrolCall> callingSessionsInBellOrder() {
            return new ArrayList<>(calls);
        }
    }

    static final class FakeSpeech implements VoicePatrolSpeech {
        final List<String> spokenTexts = new ArrayList<>();
        final List<Runnable> completions = new ArrayList<>();
        final List<String> events = new ArrayList<>();

        @Override
        public void speak(String text, Runnable onDone) {
            spokenTexts.add(text);
            completions.add(onDone);
            events.add("speak:" + text);
        }

        void finishLastUtterance() {
            Runnable onDone = completions.remove(completions.size() - 1);
            events.add("speech-done");
            onDone.run();
        }

        @Override
        public void stop() {
            events.add("speech-stop");
        }
    }

    static final class FakeSessions implements VoicePatrolSessionActions {
        final List<String> shownSessions = new ArrayList<>();
        final List<String> repliesSessionAndText = new ArrayList<>();

        @Override
        public void showSession(String sessionName) {
            shownSessions.add(sessionName);
        }

        @Override
        public void sendReply(String sessionName, String replyText) {
            repliesSessionAndText.add(sessionName + "|" + replyText);
        }
    }

    static final class FakeLog implements VoicePatrolLog {
        final List<String> warnings = new ArrayList<>();

        @Override
        public void warn(String message) {
            warnings.add(message);
        }
    }

    static final class FakeConnection implements LiveVoiceConnection {
        final String endpointUrl;
        final LiveVoiceConnectionListener listener;
        final List<String> sentEvents = new ArrayList<>();
        final List<Boolean> microphoneStates = new ArrayList<>();
        int closeCount = 0;

        FakeConnection(String endpointUrl, LiveVoiceConnectionListener listener) {
            this.endpointUrl = endpointUrl;
            this.listener = listener;
        }

        @Override
        public void sendEvent(String eventJson) {
            sentEvents.add(eventJson);
        }

        @Override
        public void setMicrophoneEnabled(boolean enabled) {
            microphoneStates.add(enabled);
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    static final class FakeConnectionFactory implements LiveVoiceConnectionFactory {
        final List<FakeConnection> opened = new ArrayList<>();

        @Override
        public LiveVoiceConnection open(String endpointUrl, LiveVoiceConnectionListener listener) {
            FakeConnection connection = new FakeConnection(endpointUrl, listener);
            opened.add(connection);
            return connection;
        }

        FakeConnection last() {
            return opened.get(opened.size() - 1);
        }
    }

    static String functionCallEvent(String callId, String name, String argumentsJson) {
        return "{\"type\":\"response.event\",\"event\":{\"type\":\"response.output_item.done\","
            + "\"item\":{\"type\":\"function_call\",\"call_id\":\"" + callId + "\",\"name\":\"" + name
            + "\",\"arguments\":" + argumentsJson + "}}}";
    }

    static String sessionStartedEvent() {
        return "{\"type\":\"session.started\"}";
    }

    static String sessionClosedEvent() {
        return "{\"type\":\"session.closed\"}";
    }
}
