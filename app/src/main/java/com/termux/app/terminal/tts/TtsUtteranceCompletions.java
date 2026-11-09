package com.termux.app.terminal.tts;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TtsUtteranceCompletions {

    private final Map<String, TtsUtteranceOutcome> mOutcomesByUtteranceId = new ConcurrentHashMap<>();

    public String register(TtsUtteranceOutcome outcome) {
        String utteranceId = UUID.randomUUID().toString();
        mOutcomesByUtteranceId.put(utteranceId, outcome);
        return utteranceId;
    }

    public boolean finishSpoken(String utteranceId) {
        TtsUtteranceOutcome outcome = mOutcomesByUtteranceId.remove(utteranceId);
        if (outcome == null) {
            return false;
        }
        outcome.onSpoken();
        return true;
    }

    public boolean finishFailed(String utteranceId, String reason) {
        TtsUtteranceOutcome outcome = mOutcomesByUtteranceId.remove(utteranceId);
        if (outcome == null) {
            return false;
        }
        outcome.onFailed(reason);
        return true;
    }
}
