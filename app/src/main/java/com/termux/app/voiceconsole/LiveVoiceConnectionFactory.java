package com.termux.app.voiceconsole;

public interface LiveVoiceConnectionFactory {

    LiveVoiceConnection open(VoiceConsoleEndpoint endpoint, LiveVoiceConnectionListener listener);
}
