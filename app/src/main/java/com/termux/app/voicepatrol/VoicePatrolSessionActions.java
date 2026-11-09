package com.termux.app.voicepatrol;

public interface VoicePatrolSessionActions {

    void showSession(String sessionName);

    void sendReply(String sessionName, String replyText);
}
