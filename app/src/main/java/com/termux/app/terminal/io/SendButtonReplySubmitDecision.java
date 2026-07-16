package com.termux.app.terminal.io;

public final class SendButtonReplySubmitDecision {

    private SendButtonReplySubmitDecision() {
    }

    public static boolean shouldRecordReply(boolean ownerContentSubmitted) {
        return ownerContentSubmitted;
    }
}
