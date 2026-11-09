package com.termux.app.voicepatrol;

import android.util.Log;

public final class AndroidLogVoicePatrolLog implements VoicePatrolLog {

    private static final String LOG_TAG = "TermuxVoicePatrol";

    @Override
    public void warn(String message) {
        Log.w(LOG_TAG, message);
    }
}
