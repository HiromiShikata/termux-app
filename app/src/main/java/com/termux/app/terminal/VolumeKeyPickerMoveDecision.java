package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public final class VolumeKeyPickerMoveDecision {

    private final int highlightedSessionIndex;
    private final boolean switchImmediately;

    private VolumeKeyPickerMoveDecision(int highlightedSessionIndex, boolean switchImmediately) {
        this.highlightedSessionIndex = highlightedSessionIndex;
        this.switchImmediately = switchImmediately;
    }

    public int getHighlightedSessionIndex() {
        return highlightedSessionIndex;
    }

    public boolean shouldSwitchImmediately() {
        return switchImmediately;
    }

    @NonNull
    public static VolumeKeyPickerMoveDecision decide(boolean previewFirstEnabled,
                                                     boolean overlayShowing,
                                                     int highlightedSessionIndex,
                                                     int currentSessionIndex,
                                                     @NonNull List<Integer> navigableSessionIndexes,
                                                     boolean forward) {
        return decide(previewFirstEnabled, overlayShowing, highlightedSessionIndex, currentSessionIndex,
            navigableSessionIndexes, navigableSessionIndexes, forward);
    }

    @NonNull
    public static VolumeKeyPickerMoveDecision decide(boolean previewFirstEnabled,
                                                     boolean overlayShowing,
                                                     int highlightedSessionIndex,
                                                     int currentSessionIndex,
                                                     @NonNull List<Integer> orderedSessionIndexes,
                                                     @NonNull List<Integer> navigableSessionIndexes,
                                                     boolean forward) {
        if (previewFirstEnabled) {
            int nextIndex = VolumeKeyPickerStep.nextHighlightedSessionIndex(
                overlayShowing, highlightedSessionIndex, currentSessionIndex,
                orderedSessionIndexes, navigableSessionIndexes, forward);
            return new VolumeKeyPickerMoveDecision(nextIndex, false);
        }
        int anchorIndex = overlayShowing ? highlightedSessionIndex : currentSessionIndex;
        int nextIndex = VolumeKeyPickerStep.nextHighlightedSessionIndex(
            true, anchorIndex, currentSessionIndex, orderedSessionIndexes, navigableSessionIndexes, forward);
        return new VolumeKeyPickerMoveDecision(nextIndex, true);
    }
}
