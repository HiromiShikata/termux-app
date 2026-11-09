package com.termux.app.voicepatrol;

public interface LiveVoiceConnection {

    void sendEvent(String eventJson);

    void setMicrophoneEnabled(boolean enabled);

    void close();
}
