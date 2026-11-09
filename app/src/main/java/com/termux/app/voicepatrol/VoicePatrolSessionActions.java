package com.termux.app.voicepatrol;

public interface VoicePatrolSessionActions {

    void showSession(String sessionName);

    boolean sendReply(String sessionName, String replyText);
}
