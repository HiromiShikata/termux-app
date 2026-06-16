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
        return nextHighlightedSessionIndex(overlayShowing, highlightedSessionIndex, currentSessionIndex,
            visibleSessionIndexes, visibleSessionIndexes, forward);
    }

    public static int nextHighlightedSessionIndex(boolean overlayShowing,
                                                  int highlightedSessionIndex,
                                                  int currentSessionIndex,
                                                  @NonNull List<Integer> orderedSessionIndexes,
                                                  @NonNull List<Integer> navigableSessionIndexes,
                                                  boolean forward) {
        if (navigableSessionIndexes.isEmpty()) {
            return currentSessionIndex;
        }
        if (!overlayShowing) {
            if (navigableSessionIndexes.contains(currentSessionIndex)) {
                return currentSessionIndex;
            }
            return VisibleSessionNavigator.nextSessionIndex(
                orderedSessionIndexes, navigableSessionIndexes, currentSessionIndex, forward);
        }
        return VisibleSessionNavigator.nextSessionIndex(
            orderedSessionIndexes, navigableSessionIndexes, highlightedSessionIndex, forward);
    }
}
