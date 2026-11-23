package com.termux.app.voiceconsole;

public enum HandshakeStepKind {
    NONE,
    SEND_OFFER,
    APPLY_ANSWER,
    FAIL,
    CONNECTED,
    DISCONNECTED
}
