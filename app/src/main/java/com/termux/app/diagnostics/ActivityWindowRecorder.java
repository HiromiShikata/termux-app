package com.termux.app.diagnostics;

import android.view.View;

import androidx.annotation.NonNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class ActivityWindowRecorder {

    private int mCreatedCount;

    private int mDestroyedCount;

    @NonNull
    private final List<Reference<View>> mWindowRoots = new ArrayList<>();

    public synchronized void recordActivityCreated() {
        mCreatedCount++;
    }

    public synchronized void recordActivityDestroyed() {
        mDestroyedCount++;
    }

    public synchronized void recordWindowRoot(@NonNull View windowRoot) {
        recordWindowRootReference(new WeakReference<>(windowRoot));
    }

    synchronized void recordWindowRootReference(@NonNull Reference<View> windowRoot) {
        View recordedRoot = windowRoot.get();
        for (Reference<View> knownRoot : mWindowRoots) {
            if (recordedRoot != null && knownRoot.get() == recordedRoot) return;
        }
        mWindowRoots.add(windowRoot);
    }

    @NonNull
    public synchronized ActivityWindowRoots snapshotWindowRoots() {
        List<View> reachableWindowRoots = new ArrayList<>();
        int noLongerReachableCount = 0;
        for (Reference<View> windowRoot : mWindowRoots) {
            View view = windowRoot.get();
            if (view == null) {
                noLongerReachableCount++;
            } else {
                reachableWindowRoots.add(view);
            }
        }
        return new ActivityWindowRoots(reachableWindowRoots, noLongerReachableCount);
    }

    @NonNull
    public synchronized DiagnosticsActivityWindows snapshot() {
        return new DiagnosticsActivityWindows(mCreatedCount, mDestroyedCount);
    }
}
