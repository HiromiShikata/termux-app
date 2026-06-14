package com.termux.app.terminal.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SpeakTagTtsController {

    private static final String LOG_TAG = "SpeakTagTtsController";

    private static final String UTTERANCE_ID = "speak_tag";

    private enum State {INITIALIZING, READY, LANGUAGE_UNAVAILABLE, FAILED}

    private final TermuxAppSharedPreferences mPreferences;

    private final Map<String, SpeakTagScanner> mScannerBySessionKey = new HashMap<>();

    private final TextToSpeech mTextToSpeech;

    private volatile State mState = State.INITIALIZING;

    private volatile String mPendingSpokenText;

    public SpeakTagTtsController(Context context) {
        mPreferences = TermuxAppSharedPreferences.build(context, true);
        mTextToSpeech = new TextToSpeech(context.getApplicationContext(), this::onTextToSpeechInit);
    }

    private void onTextToSpeechInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            mState = State.FAILED;
            Logger.logError(LOG_TAG, "Text-to-speech engine failed to initialize with status " + status);
            return;
        }

        int languageResult = mTextToSpeech.setLanguage(Locale.JAPANESE);
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            mState = State.LANGUAGE_UNAVAILABLE;
            Logger.logWarn(LOG_TAG, "Japanese text-to-speech voice is unavailable; spoken summaries will not be read aloud");
            return;
        }

        mState = State.READY;
        if (mPendingSpokenText != null) {
            dispatch(mPendingSpokenText);
            mPendingSpokenText = null;
        }
    }

    public boolean isAutoReadEnabled() {
        return mPreferences.isSpeakTagAutoReadEnabled();
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;
        if (!isAutoReadEnabled()) return;
        if (mState == State.FAILED || mState == State.LANGUAGE_UNAVAILABLE) return;

        SpeakTagScanner scanner = scannerForSession(sessionKey);
        String spokenText = scanner.newSpokenText(screenText);
        if (spokenText == null) return;

        scanner.markSpoken(spokenText);
        if (mState == State.READY) {
            dispatch(spokenText);
        } else {
            mPendingSpokenText = spokenText;
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    public void shutdown() {
        mTextToSpeech.stop();
        mTextToSpeech.shutdown();
    }

    private SpeakTagScanner scannerForSession(String sessionKey) {
        SpeakTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new SpeakTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }

    private void dispatch(String spokenText) {
        mTextToSpeech.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }
}
