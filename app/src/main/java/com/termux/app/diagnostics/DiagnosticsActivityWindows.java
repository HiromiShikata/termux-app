package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsActivityWindows {

    public static final DiagnosticsActivityWindows NONE = new DiagnosticsActivityWindows(0, 0);

    private final int mCreatedCount;

    private final int mDestroyedCount;

    @NonNull
    private final DiagnosticsWindowCondition mCondition;

    @NonNull
    private final List<ScrollWithoutDrawEpisode> mScrollWithoutDrawEpisodes;

    public DiagnosticsActivityWindows(int createdCount, int destroyedCount) {
        this(createdCount, destroyedCount, DiagnosticsWindowCondition.UNMEASURED,
            new ArrayList<ScrollWithoutDrawEpisode>());
    }

    private DiagnosticsActivityWindows(int createdCount, int destroyedCount,
                                       @NonNull DiagnosticsWindowCondition condition,
                                       @NonNull List<ScrollWithoutDrawEpisode> episodes) {
        mCreatedCount = createdCount;
        mDestroyedCount = destroyedCount;
        mCondition = condition;
        mScrollWithoutDrawEpisodes = Collections.unmodifiableList(new ArrayList<>(episodes));
    }

    @NonNull
    public DiagnosticsActivityWindows withCondition(@NonNull DiagnosticsWindowCondition condition) {
        return new DiagnosticsActivityWindows(mCreatedCount, mDestroyedCount, condition,
            mScrollWithoutDrawEpisodes);
    }

    @NonNull
    public DiagnosticsActivityWindows withScrollWithoutDrawEpisodes(
            @NonNull List<ScrollWithoutDrawEpisode> episodes) {
        return new DiagnosticsActivityWindows(mCreatedCount, mDestroyedCount, mCondition, episodes);
    }

    @NonNull
    public List<ScrollWithoutDrawEpisode> getScrollWithoutDrawEpisodes() {
        return mScrollWithoutDrawEpisodes;
    }

    public int getCreatedCount() {
        return mCreatedCount;
    }

    public int getDestroyedCount() {
        return mDestroyedCount;
    }

    public int getTeardownNotRunCount() {
        return mCreatedCount - mDestroyedCount;
    }

    @NonNull
    public DiagnosticsWindowCondition getCondition() {
        return mCondition;
    }
}
