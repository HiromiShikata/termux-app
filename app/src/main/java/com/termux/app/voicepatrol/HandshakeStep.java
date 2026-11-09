package com.termux.app.voicepatrol;

public final class HandshakeStep {

    private final HandshakeStepKind mKind;
    private final String mSdp;
    private final String mReason;

    private HandshakeStep(HandshakeStepKind kind, String sdp, String reason) {
        mKind = kind;
        mSdp = sdp;
        mReason = reason;
    }

    static HandshakeStep none() {
        return new HandshakeStep(HandshakeStepKind.NONE, "", "");
    }

    static HandshakeStep sendOffer() {
        return new HandshakeStep(HandshakeStepKind.SEND_OFFER, "", "");
    }

    static HandshakeStep applyAnswer(String sdp) {
        return new HandshakeStep(HandshakeStepKind.APPLY_ANSWER, sdp, "");
    }

    static HandshakeStep fail(String reason) {
        return new HandshakeStep(HandshakeStepKind.FAIL, "", reason);
    }

    static HandshakeStep connected() {
        return new HandshakeStep(HandshakeStepKind.CONNECTED, "", "");
    }

    static HandshakeStep disconnected(String reason) {
        return new HandshakeStep(HandshakeStepKind.DISCONNECTED, "", reason);
    }

    public HandshakeStepKind getKind() {
        return mKind;
    }

    public String getSdp() {
        return mSdp;
    }

    public String getReason() {
        return mReason;
    }
}
