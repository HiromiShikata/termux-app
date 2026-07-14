package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ParsedStatuslineUpdate {

    @NonNull
    private final String mSessionName;

    @Nullable
    private final Long mCallTimeMillis;

    @Nullable
    private final Long mOutTimeMillis;

    @Nullable
    private final Long mReplyTimeMillis;

    private final boolean mCallTimeFromDatedToken;

    private final boolean mOutTimeFromDatedToken;

    private final boolean mReplyTimeFromDatedToken;

    private final int mSubagentCount;

    public ParsedStatuslineUpdate(@NonNull String sessionName,
                                  @Nullable Long callTimeMillis,
                                  @Nullable Long outTimeMillis,
                                  @Nullable Long replyTimeMillis,
                                  int subagentCount) {
        this(sessionName, callTimeMillis, outTimeMillis, replyTimeMillis, subagentCount,
            false, false, false);
    }

    public ParsedStatuslineUpdate(@NonNull String sessionName,
                                  @Nullable Long callTimeMillis,
                                  @Nullable Long outTimeMillis,
                                  @Nullable Long replyTimeMillis,
                                  int subagentCount,
                                  boolean callTimeFromDatedToken,
                                  boolean outTimeFromDatedToken,
                                  boolean replyTimeFromDatedToken) {
        mSessionName = sessionName;
        mCallTimeMillis = callTimeMillis;
        mOutTimeMillis = outTimeMillis;
        mReplyTimeMillis = replyTimeMillis;
        mSubagentCount = subagentCount;
        mCallTimeFromDatedToken = callTimeFromDatedToken;
        mOutTimeFromDatedToken = outTimeFromDatedToken;
        mReplyTimeFromDatedToken = replyTimeFromDatedToken;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
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

    public boolean isCallTimeFromDatedToken() {
        return mCallTimeFromDatedToken;
    }

    public boolean isOutTimeFromDatedToken() {
        return mOutTimeFromDatedToken;
    }

    public boolean isReplyTimeFromDatedToken() {
        return mReplyTimeFromDatedToken;
    }

    public int getSubagentCount() {
        return mSubagentCount;
    }

    public void applyTo(@NonNull SessionNewActivityStore store) {
        store.recordStatuslineTimes(mSessionName, mCallTimeMillis, mOutTimeMillis, mReplyTimeMillis,
            mSubagentCount, mCallTimeFromDatedToken, mOutTimeFromDatedToken, mReplyTimeFromDatedToken);
    }
}
