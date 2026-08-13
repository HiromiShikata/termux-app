package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DisplayedSessionOrderBeforeRebuild {

    @NonNull
    public static final DisplayedSessionOrderBeforeRebuild NOT_CAPTURED =
        new DisplayedSessionOrderBeforeRebuild(Collections.emptyList(), null);

    @NonNull
    private final List<String> mDisplayedSessionNames;

    @Nullable
    private final String mDisplayedSessionName;

    private DisplayedSessionOrderBeforeRebuild(@NonNull List<String> displayedSessionNames,
                                               @Nullable String displayedSessionName) {
        mDisplayedSessionNames = Collections.unmodifiableList(new ArrayList<>(displayedSessionNames));
        mDisplayedSessionName = displayedSessionName;
    }

    @NonNull
    public static DisplayedSessionOrderBeforeRebuild of(@NonNull List<String> displayedSessionNames,
                                                        @Nullable String displayedSessionName) {
        return new DisplayedSessionOrderBeforeRebuild(displayedSessionNames, displayedSessionName);
    }

    @NonNull
    public List<String> getDisplayedSessionNames() {
        return mDisplayedSessionNames;
    }

    public int getPositionOfDisplayedSession() {
        if (mDisplayedSessionName == null) return NearestNeighborSessionOrder.NO_POSITION;
        return mDisplayedSessionNames.indexOf(mDisplayedSessionName);
    }
}
