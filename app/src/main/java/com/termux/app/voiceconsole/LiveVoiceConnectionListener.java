package com.termux.app.voiceconsole;

public interface LiveVoiceConnectionListener {

    void onConnected();

    void onEventReceived(String eventJson);

    void onConnectFailed(String reason);

    void onDisconnected(String reason);
}
