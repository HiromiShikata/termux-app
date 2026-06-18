package com.termux.terminal;

public final class SpeakTagFilter {

    public interface DisplayEmitter {
        void emitCodePoint(int codePoint);
    }

    public interface SpeakListener {
        void onSpeakText(String text);
    }

    private static final String OPEN_MARKER = "<speak>";
    private static final String CLOSE_MARKER = "</speak>";

    static final int MAX_SPOKEN_TEXT_LENGTH = 500;

    private final StringBuilder mPendingMarker = new StringBuilder();
    private final StringBuilder mSpokenText = new StringBuilder();
    private boolean mInsideSpeak = false;

    public void processCodePoint(int codePoint, DisplayEmitter display, SpeakListener speak) {
        mPendingMarker.appendCodePoint(codePoint);

        while (mPendingMarker.length() > 0 && !isPrefixOfActiveMarker(mPendingMarker)) {
            releaseFirstPendingCodePoint(display);
        }

        if (markerComplete(mPendingMarker, OPEN_MARKER)) {
            mPendingMarker.setLength(0);
            mSpokenText.setLength(0);
            mInsideSpeak = true;
        } else if (mInsideSpeak && markerComplete(mPendingMarker, CLOSE_MARKER)) {
            mPendingMarker.setLength(0);
            String text = mSpokenText.toString();
            mSpokenText.setLength(0);
            mInsideSpeak = false;
            if (!text.isEmpty()) speak.onSpeakText(text);
        } else if (mInsideSpeak && mSpokenText.length() > MAX_SPOKEN_TEXT_LENGTH) {
            abortSpeak(display);
        }
    }

    public void flushPending(DisplayEmitter display) {
        while (mPendingMarker.length() > 0) {
            releaseFirstPendingCodePoint(display);
        }
    }

    public void abortSpeak(DisplayEmitter display) {
        if (!mInsideSpeak) return;
        mInsideSpeak = false;
        flushPending(display);
        mSpokenText.setLength(0);
    }

    public boolean hasPending() {
        return mPendingMarker.length() > 0;
    }

    private void releaseFirstPendingCodePoint(DisplayEmitter display) {
        int codePoint = mPendingMarker.codePointAt(0);
        mPendingMarker.delete(0, Character.charCount(codePoint));
        display.emitCodePoint(codePoint);
        if (mInsideSpeak) mSpokenText.appendCodePoint(codePoint);
    }

    private boolean isPrefixOfActiveMarker(CharSequence candidate) {
        if (isPrefixOfMarker(candidate, OPEN_MARKER)) return true;
        return mInsideSpeak && isPrefixOfMarker(candidate, CLOSE_MARKER);
    }

    private static boolean markerComplete(CharSequence candidate, String marker) {
        if (candidate.length() != marker.length()) return false;
        return isPrefixOfMarker(candidate, marker);
    }

    private static boolean isPrefixOfMarker(CharSequence candidate, String marker) {
        if (candidate.length() > marker.length()) return false;
        for (int i = 0; i < candidate.length(); i++) {
            if (candidate.charAt(i) != marker.charAt(i)) return false;
        }
        return true;
    }
}
