package com.termux.app.voicepatrol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VoicePatrolTerminalSessions {

    List<String> sessionNames();

    List<Integer> orderedSessionIndexes();

    Set<String> pendingCallSessionNames();

    Map<String, String> unansweredCallBodiesBySessionName();

    boolean isActivityVisible();

    void switchToSessionAtIndex(int sessionIndex);

    boolean submitVoiceReplyToCurrentSession(String replyText);
}
