package com.termux.app.voicepatrol;

import java.util.List;

public final class TermuxVoicePatrolPorts implements VoicePatrolCallSource, VoicePatrolSessionActions {

    private final VoicePatrolActivityBridge mBridge;
    private final VoicePatrolLog mLog;

    public TermuxVoicePatrolPorts(VoicePatrolActivityBridge bridge, VoicePatrolLog log) {
        mBridge = bridge;
        mLog = log;
    }

    @Override
    public List<VoicePatrolCall> callingSessionsInBellOrder() {
        return mBridge.callingSessionsInBellOrder();
    }

    @Override
    public void showSession(String sessionName) {
        mBridge.showSession(sessionName);
    }

    @Override
    public void sendReply(String sessionName, String replyText) {
        if (!mBridge.sendReply(sessionName, replyText)) {
            mLog.warn("The reply to " + sessionName + " could not be delivered to its terminal");
        }
    }
}
