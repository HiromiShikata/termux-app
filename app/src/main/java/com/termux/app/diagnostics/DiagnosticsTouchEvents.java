package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.touch.TerminalTouchCounter;
import com.termux.view.touch.TerminalTouchKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsTouchEvents {

    public static final DiagnosticsTouchEvents NONE =
        new DiagnosticsTouchEvents(Collections.<DiagnosticsTouchCount>emptyList());

    @NonNull
    private final List<DiagnosticsTouchCount> mCountsByKind;

    public DiagnosticsTouchEvents(@NonNull List<DiagnosticsTouchCount> countsByKind) {
        mCountsByKind = Collections.unmodifiableList(countsByKind);
    }

    @NonNull
    public static DiagnosticsTouchEvents of(@NonNull TerminalTouchCounter counter) {
        List<DiagnosticsTouchCount> countsByKind = new ArrayList<>();
        for (TerminalTouchKind kind : TerminalTouchKind.values()) {
            int touchCount = counter.getTouchCount(kind);
            if (touchCount > 0) {
                countsByKind.add(new DiagnosticsTouchCount(kind, touchCount,
                    counter.getLastTouchAtMillis(kind)));
            }
        }
        return new DiagnosticsTouchEvents(countsByKind);
    }

    @NonNull
    public List<DiagnosticsTouchCount> getCountsByKind() {
        return mCountsByKind;
    }

    public int getTotalTouchCount() {
        int totalTouchCount = 0;
        for (DiagnosticsTouchCount countByKind : mCountsByKind) {
            totalTouchCount += countByKind.getTouchCount();
        }
        return totalTouchCount;
    }
}
