package com.termux.app.terminal.tts;

public interface TtsUtteranceOutcome {

    void onSpoken();

    void onFailed(String reason);
}
