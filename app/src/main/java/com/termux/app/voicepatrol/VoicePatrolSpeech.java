package com.termux.app.voicepatrol;

public interface VoicePatrolSpeech {

    void speak(String text, Runnable onDone);

    void stop();
}
