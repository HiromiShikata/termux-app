package com.termux.app.voiceconsole;

public interface LiveVoiceConnection {

    void sendEvent(String eventJson);

    void close();
}
