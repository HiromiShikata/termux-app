package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsSessionLine {

    @NonNull
    private final String mName;
    private final boolean mAlive;
    private final long mSecondsSinceLastActivity;
    private final boolean mHasLastActivity;

    public DiagnosticsSessionLine(@NonNull String name, boolean alive, long secondsSinceLastActivity,
                                  boolean hasLastActivity) {
        mName = name;
        mAlive = alive;
        mSecondsSinceLastActivity = secondsSinceLastActivity;
        mHasLastActivity = hasLastActivity;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public boolean isAlive() {
        return mAlive;
    }

    public long getSecondsSinceLastActivity() {
        return mSecondsSinceLastActivity;
    }

    public boolean hasLastActivity() {
        return mHasLastActivity;
    }
}
