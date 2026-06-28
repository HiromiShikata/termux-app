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

    private final int mSubagentCount;

    public ParsedStatuslineUpdate(@NonNull String sessionName,
                                  @Nullable Long callTimeMillis,
                                  @Nullable Long outTimeMillis,
                                  @Nullable Long replyTimeMillis,
                                  int subagentCount) {
        mSessionName = sessionName;
        mCallTimeMillis = callTimeMillis;
        mOutTimeMillis = outTimeMillis;
        mReplyTimeMillis = replyTimeMillis;
        mSubagentCount = subagentCount;
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

    public int getSubagentCount() {
        return mSubagentCount;
    }

    public void applyTo(@NonNull SessionNewActivityStore store) {
        store.recordStatuslineTimes(mSessionName, mCallTimeMillis, mOutTimeMillis, mReplyTimeMillis,
            mSubagentCount);
    }
}
