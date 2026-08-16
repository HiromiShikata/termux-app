package com.termux.app.diagnostics;

import androidx.annotation.Nullable;

public final class DiagnosticsScrollAnswer {

    private final long mEpisodesSentToTheProgram;
    private final long mEpisodesAnsweredByTheProgram;

    @Nullable
    private final Long mUnansweredEpisodeSentAtMillis;

    @Nullable
    private final Long mLastEpisodeAnsweredAtMillis;

    public DiagnosticsScrollAnswer(long episodesSentToTheProgram,
                                   long episodesAnsweredByTheProgram,
                                   @Nullable Long unansweredEpisodeSentAtMillis,
                                   @Nullable Long lastEpisodeAnsweredAtMillis) {
        mEpisodesSentToTheProgram = episodesSentToTheProgram;
        mEpisodesAnsweredByTheProgram = episodesAnsweredByTheProgram;
        mUnansweredEpisodeSentAtMillis = unansweredEpisodeSentAtMillis;
        mLastEpisodeAnsweredAtMillis = lastEpisodeAnsweredAtMillis;
    }

    public long getEpisodesSentToTheProgram() {
        return mEpisodesSentToTheProgram;
    }

    public long getEpisodesAnsweredByTheProgram() {
        return mEpisodesAnsweredByTheProgram;
    }

    @Nullable
    public Long getUnansweredEpisodeSentAtMillis() {
        return mUnansweredEpisodeSentAtMillis;
    }

    @Nullable
    public Long getLastEpisodeAnsweredAtMillis() {
        return mLastEpisodeAnsweredAtMillis;
    }

    public boolean hasBeenSentAScroll() {
        return mEpisodesSentToTheProgram > 0;
    }
}
