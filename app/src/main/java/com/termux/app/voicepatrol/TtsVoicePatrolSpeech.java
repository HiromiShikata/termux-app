package com.termux.app.voicepatrol;

import com.termux.app.terminal.tts.TtsManager;

public final class TtsVoicePatrolSpeech implements VoicePatrolSpeech {

    private final TtsManager mTtsManager;

    public TtsVoicePatrolSpeech(TtsManager ttsManager) {
        mTtsManager = ttsManager;
    }

    @Override
    public void speak(String text, Runnable onDone) {
        mTtsManager.speak(text, onDone);
    }

    @Override
    public void stop() {
        mTtsManager.stop();
    }
}
