package com.termux.app.voicepatrol;

public interface LiveVoiceConnectionFactory {

    LiveVoiceConnection open(String endpointUrl, LiveVoiceConnectionListener listener);
}
