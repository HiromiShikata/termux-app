package com.termux.app.voicepatrol;

import java.util.List;

public interface VoicePatrolActivityBridge {

    List<VoicePatrolCall> callingSessionsInBellOrder();

    void showSession(String sessionName);

    boolean sendReply(String sessionName, String replyText);
}
