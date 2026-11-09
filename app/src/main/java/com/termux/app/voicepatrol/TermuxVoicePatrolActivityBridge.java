package com.termux.app.voicepatrol;

import java.util.List;

public final class TermuxVoicePatrolActivityBridge implements VoicePatrolActivityBridge {

    private final VoicePatrolTerminalSessions mSessions;

    public TermuxVoicePatrolActivityBridge(VoicePatrolTerminalSessions sessions) {
        mSessions = sessions;
    }

    @Override
    public List<VoicePatrolCall> callingSessionsInBellOrder() {
        return VoicePatrolSessionSelection.callsInBellOrder(
            mSessions.orderedSessionIndexes(),
            mSessions.sessionNames(),
            mSessions.pendingCallSessionNames(),
            mSessions.unansweredCallBodiesBySessionName());
    }

    @Override
    public void showSession(String sessionName) {
        if (!mSessions.isActivityVisible()) {
            return;
        }
        int sessionIndex = VoicePatrolSessionSelection.indexOfSessionName(mSessions.sessionNames(), sessionName);
        if (sessionIndex >= 0) {
            mSessions.switchToSessionAtIndex(sessionIndex);
        }
    }

    @Override
    public boolean sendReply(String sessionName, String replyText) {
        int sessionIndex = VoicePatrolSessionSelection.indexOfSessionName(mSessions.sessionNames(), sessionName);
        if (sessionIndex < 0) {
            return false;
        }
        mSessions.switchToSessionAtIndex(sessionIndex);
        return mSessions.submitVoiceReplyToCurrentSession(replyText);
    }
}
