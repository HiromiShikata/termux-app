package com.termux.app.terminal.io;

public final class ReconnectSubmitReplyDecision {

    private ReconnectSubmitReplyDecision() {
    }

    public static boolean shouldRecordReply(boolean reconnected, String submittedTextInput) {
        return reconnected && ToolbarTextInputEncoder.hasContentToSend(submittedTextInput);
    }
}
