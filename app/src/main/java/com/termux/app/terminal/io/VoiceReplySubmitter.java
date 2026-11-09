package com.termux.app.terminal.io;

public final class VoiceReplySubmitter {

    public interface ReplyTerminal {

        boolean acceptsOwnerInput();

        void pasteOwnerContent(String text);

        void sendEnterAfterOwnerContent();

        void recordGenuineReply();
    }

    private VoiceReplySubmitter() {
    }

    public static boolean submit(ReplyTerminal terminal, String replyText) {
        if (replyText.isEmpty() || !terminal.acceptsOwnerInput()) {
            return false;
        }
        terminal.pasteOwnerContent(ToolbarTextInputEncoder.textToSend(replyText));
        terminal.sendEnterAfterOwnerContent();
        terminal.recordGenuineReply();
        return true;
    }
}
