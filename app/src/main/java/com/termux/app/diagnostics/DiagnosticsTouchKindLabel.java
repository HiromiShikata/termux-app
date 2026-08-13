package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.touch.TerminalTouchKind;

public final class DiagnosticsTouchKindLabel {

    private DiagnosticsTouchKindLabel() {
    }

    @NonNull
    public static String of(@NonNull TerminalTouchKind kind) {
        switch (kind) {
            case GESTURE_START:
                return "the start of a gesture";
            case GESTURE_MOVEMENT:
                return "movement within a gesture";
            case GESTURE_END:
                return "the end of a gesture";
            case ANOTHER_KIND:
                return "another kind of touch";
        }
        throw new IllegalStateException("no wording is defined for the touch kind " + kind);
    }
}
