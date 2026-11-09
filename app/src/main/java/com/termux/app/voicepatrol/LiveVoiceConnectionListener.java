package com.termux.app.voicepatrol;

public interface LiveVoiceConnectionListener {

    void onEventReceived(String eventJson);

    void onConnectFailed(String reason);

    void onDisconnected(String reason);
}
