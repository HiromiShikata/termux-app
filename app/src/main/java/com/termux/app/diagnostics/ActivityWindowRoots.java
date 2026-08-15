package com.termux.app.diagnostics;

import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActivityWindowRoots {

    @NonNull
    private final List<View> mReachableWindowRoots;

    private final int mNoLongerReachableCount;

    public ActivityWindowRoots(@NonNull List<View> reachableWindowRoots, int noLongerReachableCount) {
        mReachableWindowRoots = Collections.unmodifiableList(new ArrayList<>(reachableWindowRoots));
        mNoLongerReachableCount = noLongerReachableCount;
    }

    @NonNull
    public List<View> getReachableWindowRoots() {
        return mReachableWindowRoots;
    }

    public int getNoLongerReachableCount() {
        return mNoLongerReachableCount;
    }
}
