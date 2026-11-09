package com.termux.app.voicepatrol;

import com.termux.app.TermuxActivity;

import java.util.List;

public final class TermuxVoicePatrolPorts implements VoicePatrolCallSource, VoicePatrolSessionActions {

    private final TermuxActivity mActivity;
    private final VoicePatrolLog mLog;

    public TermuxVoicePatrolPorts(TermuxActivity activity, VoicePatrolLog log) {
        mActivity = activity;
        mLog = log;
    }

    @Override
    public List<VoicePatrolCall> callingSessionsInBellOrder() {
        return mActivity.voicePatrolCallsInBellOrder();
    }

    @Override
    public void showSession(String sessionName) {
        mActivity.showVoicePatrolSession(sessionName);
    }

    @Override
    public void sendReply(String sessionName, String replyText) {
        if (!mActivity.sendVoicePatrolReply(sessionName, replyText)) {
            mLog.warn("The reply to " + sessionName + " could not be delivered to its terminal");
        }
    }
}
