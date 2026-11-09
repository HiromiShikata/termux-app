package com.termux.app.voicepatrol;

import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.terminal.tts.TtsUtteranceOutcome;

public final class TtsVoicePatrolSpeech implements VoicePatrolSpeech {

    private final TtsManager mTtsManager;

    public TtsVoicePatrolSpeech(TtsManager ttsManager) {
        mTtsManager = ttsManager;
    }

    @Override
    public void speak(String text, VoicePatrolSpeechOutcome outcome) {
        mTtsManager.speak(text, new TtsUtteranceOutcome() {
            @Override
            public void onSpoken() {
                outcome.onSpoken();
            }

            @Override
            public void onFailed(String reason) {
                outcome.onFailed(reason);
            }
        });
    }

    @Override
    public void stop() {
        mTtsManager.stop();
    }
}
