package com.termux.terminal;

import androidx.annotation.Nullable;

public final class ScrollAnswerRecord {

    private long mEpisodesSentToTheProgram;
    private long mEpisodesAnsweredByTheProgram;

    @Nullable
    private Long mUnansweredEpisodeSentAtMillis;

    @Nullable
    private Long mLastEpisodeAnsweredAtMillis;

    public synchronized void recordScrollSentToTheProgram(long sentAtMillis) {
        if (mUnansweredEpisodeSentAtMillis != null) return;
        mEpisodesSentToTheProgram++;
        mUnansweredEpisodeSentAtMillis = sentAtMillis;
    }

    public synchronized void recordOutputFromTheProgram(long producedAtMillis) {
        if (mUnansweredEpisodeSentAtMillis == null) return;
        mEpisodesAnsweredByTheProgram++;
        mUnansweredEpisodeSentAtMillis = null;
        mLastEpisodeAnsweredAtMillis = producedAtMillis;
    }

    public synchronized long getEpisodesSentToTheProgram() {
        return mEpisodesSentToTheProgram;
    }

    public synchronized long getEpisodesAnsweredByTheProgram() {
        return mEpisodesAnsweredByTheProgram;
    }

    @Nullable
    public synchronized Long getUnansweredEpisodeSentAtMillis() {
        return mUnansweredEpisodeSentAtMillis;
    }

    @Nullable
    public synchronized Long getLastEpisodeAnsweredAtMillis() {
        return mLastEpisodeAnsweredAtMillis;
    }
}
