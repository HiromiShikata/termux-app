package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallBodyRange {

    private final int mStartIndex;
    private final int mEndIndex;

    @NonNull
    private final String mText;

    public OwnerCallBodyRange(int startIndex, int endIndex, @NonNull String text) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mText = text;
    }

    public int getStartIndex() {
        return mStartIndex;
    }

    public int getEndIndex() {
        return mEndIndex;
    }

    @NonNull
    public String getText() {
        return mText;
    }
}
