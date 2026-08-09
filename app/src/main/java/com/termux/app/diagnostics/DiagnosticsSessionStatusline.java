package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.SessionNewActivityTier;

/**
 * The statusline {@code call:}/{@code out:}/{@code reply:} times the app is currently holding for a
 * session and the activity tier those held values resolve to. A session whose own screen renders a
 * {@code call:} newer than its {@code reply:} must show the RED dot, so an observed grey dot has
 * exactly two possible explanations: the app never came to hold that statusline, or it held it and
 * the tier still resolved away from RED. Nothing else in the report separates them, so both the held
 * values and the tier they produce are reported for every session.
 */
public final class DiagnosticsSessionStatusline {

    @Nullable
    private final Long mCallTimeMillis;

    @Nullable
    private final Long mOutTimeMillis;

    @Nullable
    private final Long mReplyTimeMillis;

    @NonNull
    private final SessionNewActivityTier mTier;

    public DiagnosticsSessionStatusline(@Nullable Long callTimeMillis,
                                        @Nullable Long outTimeMillis,
                                        @Nullable Long replyTimeMillis,
                                        @NonNull SessionNewActivityTier tier) {
        mCallTimeMillis = callTimeMillis;
        mOutTimeMillis = outTimeMillis;
        mReplyTimeMillis = replyTimeMillis;
        mTier = tier;
    }

    @Nullable
    public Long getCallTimeMillis() {
        return mCallTimeMillis;
    }

    @Nullable
    public Long getOutTimeMillis() {
        return mOutTimeMillis;
    }

    @Nullable
    public Long getReplyTimeMillis() {
        return mReplyTimeMillis;
    }

    @NonNull
    public SessionNewActivityTier getTier() {
        return mTier;
    }

    public boolean isHeld() {
        return mCallTimeMillis != null || mOutTimeMillis != null || mReplyTimeMillis != null;
    }
}
