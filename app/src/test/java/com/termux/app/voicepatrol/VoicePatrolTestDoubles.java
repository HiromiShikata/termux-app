package com.termux.app.voicepatrol;

import java.util.ArrayList;
import java.util.List;

final class VoicePatrolTestDoubles {
    private VoicePatrolTestDoubles() {}

    static final class Calls implements VoicePatrolCallSource {
        List<VoicePatrolCall> calls = new ArrayList<>();
        public List<VoicePatrolCall> callingSessionsInBellOrder() { return new ArrayList<>(calls); }
    }
    static final class Speech implements VoicePatrolSpeech {
        final List<String> texts = new ArrayList<>();
        final List<VoicePatrolSpeechOutcome> outcomes = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        public void speak(String text, VoicePatrolSpeechOutcome outcome) { texts.add(text); outcomes.add(outcome); events.add("speak"); }
        public void stop() { events.add("stop"); }
        void spoken() { outcomes.remove(0).onSpoken(); }
        void failed(String reason) { outcomes.remove(0).onFailed(reason); }
    }
    static final class Actions implements VoicePatrolSessionActions {
        final List<String> shown = new ArrayList<>();
        final List<String> replies = new ArrayList<>();
        public void showSession(String name) { shown.add(name); }
        boolean replyDelivered = true;
        public boolean sendReply(String name, String text) { replies.add(name + "|" + text); return replyDelivered; }
    }
    static final class Log implements VoicePatrolLog {
        final List<String> warnings = new ArrayList<>();
        public void warn(String message) { warnings.add(message); }
    }
    static final class Connection implements LiveVoiceConnection {
        final String endpoint; final LiveVoiceConnectionListener listener;
        final List<String> events = new ArrayList<>(); final List<Boolean> microphones = new ArrayList<>(); int closes;
        Connection(String endpoint, LiveVoiceConnectionListener listener) { this.endpoint = endpoint; this.listener = listener; }
        public void sendEvent(String eventJson) { events.add(eventJson); }
        public void setMicrophoneEnabled(boolean enabled) { microphones.add(enabled); }
        public void close() { closes++; }
    }
    static final class Connections implements LiveVoiceConnectionFactory {
        final List<Connection> opened = new ArrayList<>();
        public LiveVoiceConnection open(String endpoint, LiveVoiceConnectionListener listener) { Connection c = new Connection(endpoint, listener); opened.add(c); return c; }
    }
    static String event(String type) { return "{\"type\":\"" + type + "\"}"; }
    static String function(String id, String name, String arguments) { return "{\"type\":\"response.event\",\"event\":{\"type\":\"response.output_item.done\",\"item\":{\"type\":\"function_call\",\"call_id\":\"" + id + "\",\"name\":\"" + name + "\",\"arguments\":" + arguments + "}}}"; }
}
