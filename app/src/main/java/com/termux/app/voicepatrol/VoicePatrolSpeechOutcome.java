package com.termux.app.voicepatrol;

public interface VoicePatrolSpeechOutcome {

    void onSpoken();

    void onFailed(String reason);
}
