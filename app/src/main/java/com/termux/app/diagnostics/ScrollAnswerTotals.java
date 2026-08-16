package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.terminal.ScrollAnswerRecord;

public final class ScrollAnswerTotals {

    public static final ScrollAnswerTotals NONE = new ScrollAnswerTotals(0L, 0L, 0L);

    private final long mEpisodesSentToTheProgram;

    private final long mEpisodesAnsweredByTheProgram;

    private final long mEarliestUnansweredEpisodeSentAtMillis;

    private ScrollAnswerTotals(long episodesSentToTheProgram, long episodesAnsweredByTheProgram,
                               long earliestUnansweredEpisodeSentAtMillis) {
        mEpisodesSentToTheProgram = episodesSentToTheProgram;
        mEpisodesAnsweredByTheProgram = episodesAnsweredByTheProgram;
        mEarliestUnansweredEpisodeSentAtMillis = earliestUnansweredEpisodeSentAtMillis;
    }

    @NonNull
    public static ScrollAnswerTotals of(long episodesSentToTheProgram,
                                        long episodesAnsweredByTheProgram,
                                        long earliestUnansweredEpisodeSentAtMillis) {
        return new ScrollAnswerTotals(episodesSentToTheProgram, episodesAnsweredByTheProgram,
            earliestUnansweredEpisodeSentAtMillis);
    }

    @NonNull
    public static ScrollAnswerTotals ofRecords(@NonNull Iterable<ScrollAnswerRecord> records) {
        long episodesSentToTheProgram = 0L;
        long episodesAnsweredByTheProgram = 0L;
        long earliestUnansweredEpisodeSentAtMillis = 0L;
        for (ScrollAnswerRecord record : records) {
            episodesSentToTheProgram += record.getEpisodesSentToTheProgram();
            episodesAnsweredByTheProgram += record.getEpisodesAnsweredByTheProgram();
            Long unansweredEpisodeSentAtMillis = record.getUnansweredEpisodeSentAtMillis();
            if (unansweredEpisodeSentAtMillis == null) continue;
            if (earliestUnansweredEpisodeSentAtMillis == 0L
                || unansweredEpisodeSentAtMillis < earliestUnansweredEpisodeSentAtMillis) {
                earliestUnansweredEpisodeSentAtMillis = unansweredEpisodeSentAtMillis;
            }
        }
        return new ScrollAnswerTotals(episodesSentToTheProgram, episodesAnsweredByTheProgram,
            earliestUnansweredEpisodeSentAtMillis);
    }

    public long getEpisodesSentToTheProgram() {
        return mEpisodesSentToTheProgram;
    }

    public long getEpisodesAnsweredByTheProgram() {
        return mEpisodesAnsweredByTheProgram;
    }

    public long getEarliestUnansweredEpisodeSentAtMillis() {
        return mEarliestUnansweredEpisodeSentAtMillis;
    }

    public boolean hasUnansweredEpisode() {
        return mEarliestUnansweredEpisodeSentAtMillis != 0L;
    }
}
