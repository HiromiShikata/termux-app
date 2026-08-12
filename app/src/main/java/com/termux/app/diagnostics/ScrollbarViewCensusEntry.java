package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class ScrollbarViewCensusEntry {

    @NonNull
    private final String mClassName;

    private final int mViewCount;

    public ScrollbarViewCensusEntry(@NonNull String className, int viewCount) {
        mClassName = className;
        mViewCount = viewCount;
    }

    @NonNull
    public String getClassName() {
        return mClassName;
    }

    public int getViewCount() {
        return mViewCount;
    }
}
