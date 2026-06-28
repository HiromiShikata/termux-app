package com.termux.app.terminal;

public final class CallToUserScrollAction {

    public enum Kind {
        SCROLL_TO_TAG,
        SCROLL_TO_LATEST
    }

    public static final int LATEST_OUTPUT_TOP_ROW = 0;

    private final Kind mKind;

    private final int mTargetTopRow;

    private CallToUserScrollAction(Kind kind, int targetTopRow) {
        mKind = kind;
        mTargetTopRow = targetTopRow;
    }

    public Kind getKind() {
        return mKind;
    }

    public int getTargetTopRow() {
        return mTargetTopRow;
    }

    public static CallToUserScrollAction resolve(boolean alternateBufferActive, int locatedTopRow) {
        if (!alternateBufferActive && locatedTopRow != CallToUserTagScrollLocator.NO_TAG_ROW) {
            return new CallToUserScrollAction(Kind.SCROLL_TO_TAG, locatedTopRow);
        }
        return new CallToUserScrollAction(Kind.SCROLL_TO_LATEST, LATEST_OUTPUT_TOP_ROW);
    }
}
