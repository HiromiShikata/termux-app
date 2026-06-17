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

    private final StringBuilder mPendingMarker = new StringBuilder();
    private final StringBuilder mSpokenText = new StringBuilder();
    private boolean mInsideSpeak = false;

    public void processCodePoint(int codePoint, DisplayEmitter display, SpeakListener speak) {
        String marker = mInsideSpeak ? CLOSE_MARKER : OPEN_MARKER;
        mPendingMarker.appendCodePoint(codePoint);

        while (mPendingMarker.length() > 0 && !isPrefixOfMarker(mPendingMarker, marker)) {
            releaseFirstPendingCodePoint(display);
        }

        if (mPendingMarker.length() == marker.length()) {
            mPendingMarker.setLength(0);
            if (mInsideSpeak) {
                String text = mSpokenText.toString();
                mSpokenText.setLength(0);
                mInsideSpeak = false;
                if (!text.isEmpty()) speak.onSpeakText(text);
            } else {
                mInsideSpeak = true;
            }
        }
    }

    public void flushPending(DisplayEmitter display) {
        while (mPendingMarker.length() > 0) {
            releaseFirstPendingCodePoint(display);
        }
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

    private static boolean isPrefixOfMarker(CharSequence candidate, String marker) {
        if (candidate.length() > marker.length()) return false;
        for (int i = 0; i < candidate.length(); i++) {
            if (candidate.charAt(i) != marker.charAt(i)) return false;
        }
        return true;
    }
}
