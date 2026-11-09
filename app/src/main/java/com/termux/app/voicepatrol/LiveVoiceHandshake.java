package com.termux.app.voicepatrol;

public final class LiveVoiceHandshake {

    public static final long GATHERING_TIMEOUT_MILLIS = 15_000L;

    static final String GATHERING_TIMEOUT_REASON = "ice_gathering_timeout";
    static final String REMOTE_DESCRIPTION_FAILED_REASON = "remote_description_failed";

    private boolean mLocalDescriptionSet;
    private boolean mIceGatheringComplete;
    private boolean mOfferSent;
    private boolean mAwaitingAnswer;
    private boolean mAnswerApplied;
    private boolean mConnected;
    private boolean mTerminated;

    public HandshakeStep onLocalDescriptionSet() {
        if (mTerminated) {
            return HandshakeStep.none();
        }
        mLocalDescriptionSet = true;
        return offerWhenReady();
    }

    public HandshakeStep onIceGatheringComplete() {
        if (mTerminated) {
            return HandshakeStep.none();
        }
        mIceGatheringComplete = true;
        return offerWhenReady();
    }

    public HandshakeStep onGatheringTimedOut() {
        if (mOfferSent) {
            return HandshakeStep.none();
        }
        return terminate(GATHERING_TIMEOUT_REASON);
    }

    public HandshakeStep onAnswerReceived(LiveSessionAnswer answer) {
        if (mTerminated || !mAwaitingAnswer) {
            return HandshakeStep.none();
        }
        mAwaitingAnswer = false;
        if (!answer.isAccepted()) {
            return terminate(answer.getFailureReason());
        }
        mAnswerApplied = true;
        return HandshakeStep.applyAnswer(answer.getSdp());
    }

    public HandshakeStep onRemoteDescriptionApplied() {
        return HandshakeStep.none();
    }

    public HandshakeStep onRemoteDescriptionFailed() {
        return terminate(REMOTE_DESCRIPTION_FAILED_REASON);
    }

    public HandshakeStep onNegotiationFailed(String reason) {
        return terminate(reason);
    }

    public HandshakeStep onDataChannelOpened() {
        if (mTerminated || mConnected || !mAnswerApplied) {
            return HandshakeStep.none();
        }
        mConnected = true;
        return HandshakeStep.connected();
    }

    public HandshakeStep onConnectionLost(String reason) {
        if (mTerminated) {
            return HandshakeStep.none();
        }
        if (mConnected) {
            mTerminated = true;
            return HandshakeStep.disconnected(reason);
        }
        return terminate(reason);
    }

    private HandshakeStep offerWhenReady() {
        if (!mLocalDescriptionSet || !mIceGatheringComplete || mOfferSent || mTerminated) {
            return HandshakeStep.none();
        }
        mOfferSent = true;
        mAwaitingAnswer = true;
        return HandshakeStep.sendOffer();
    }

    private HandshakeStep terminate(String reason) {
        if (mTerminated) {
            return HandshakeStep.none();
        }
        mTerminated = true;
        return HandshakeStep.fail(reason);
    }
}
