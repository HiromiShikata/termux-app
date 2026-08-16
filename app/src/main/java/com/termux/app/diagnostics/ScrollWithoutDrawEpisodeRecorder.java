package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScrollWithoutDrawEpisodeRecorder {

    public static final long UNDRAWN_AFTER_SCROLL_THRESHOLD_MILLIS = 2000L;

    public static final int MAX_RETAINED_EPISODES = 3;

    private final List<ScrollWithoutDrawEpisode> mEpisodes = new ArrayList<>();

    private boolean mHasRecordedEpisode;

    private long mRecordedTerminalDrawAtElapsedRealtimeMillis;

    public synchronized boolean recordEpisode(long lastScrollStepAtMillis,
                                              long lastTerminalDrawAtMillis,
                                              long lastTerminalDrawAtElapsedRealtimeMillis) {
        long undrawnForMillis = lastScrollStepAtMillis - lastTerminalDrawAtMillis;
        if (undrawnForMillis < UNDRAWN_AFTER_SCROLL_THRESHOLD_MILLIS) {
            return false;
        }
        if (mHasRecordedEpisode && mRecordedTerminalDrawAtElapsedRealtimeMillis
                == lastTerminalDrawAtElapsedRealtimeMillis) {
            return false;
        }
        mHasRecordedEpisode = true;
        mRecordedTerminalDrawAtElapsedRealtimeMillis = lastTerminalDrawAtElapsedRealtimeMillis;
        mEpisodes.add(new ScrollWithoutDrawEpisode(lastScrollStepAtMillis, undrawnForMillis));
        while (mEpisodes.size() > MAX_RETAINED_EPISODES) {
            mEpisodes.remove(0);
        }
        return true;
    }

    @NonNull
    public synchronized List<ScrollWithoutDrawEpisode> getEpisodes() {
        return Collections.unmodifiableList(new ArrayList<>(mEpisodes));
    }
}
