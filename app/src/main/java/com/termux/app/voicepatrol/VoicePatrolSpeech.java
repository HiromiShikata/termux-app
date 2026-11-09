package com.termux.app.voicepatrol;

public interface VoicePatrolSpeech {

    void speak(String text, VoicePatrolSpeechOutcome outcome);

    void stop();
}
