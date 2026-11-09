package com.termux.app.voicepatrol;

public enum HandshakeStepKind {
    NONE,
    SEND_OFFER,
    APPLY_ANSWER,
    FAIL,
    CONNECTED,
    DISCONNECTED
}
