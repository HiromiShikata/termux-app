package com.termux.app.voiceconsole;

public interface LiveVoiceConnectionFactory {

    LiveVoiceConnection open(String endpointUrl, LiveVoiceConnectionListener listener);
}
