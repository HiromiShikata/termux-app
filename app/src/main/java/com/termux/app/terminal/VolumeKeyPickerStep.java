package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public final class VolumeKeyPickerStep {

    private VolumeKeyPickerStep() {
    }

    public static int nextHighlightedSessionIndex(boolean overlayShowing,
                                                  int highlightedSessionIndex,
                                                  int currentSessionIndex,
                                                  @NonNull List<Integer> visibleSessionIndexes,
                                                  boolean forward) {
        if (visibleSessionIndexes.isEmpty()) {
            return currentSessionIndex;
        }
        if (!overlayShowing) {
            if (visibleSessionIndexes.contains(currentSessionIndex)) {
                return currentSessionIndex;
            }
            return SessionHierarchyBuilder.nextVisibleSessionIndex(
                visibleSessionIndexes, currentSessionIndex, forward);
        }
        return SessionHierarchyBuilder.nextVisibleSessionIndex(
            visibleSessionIndexes, highlightedSessionIndex, forward);
    }
}
