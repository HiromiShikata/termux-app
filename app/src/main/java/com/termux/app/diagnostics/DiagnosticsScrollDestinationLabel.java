package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.scroll.TerminalScrollEvent;

public final class DiagnosticsScrollDestinationLabel {

    private DiagnosticsScrollDestinationLabel() {
    }

    @NonNull
    public static String of(@NonNull TerminalScrollEvent destination) {
        switch (destination) {
            case MOUSE_WHEEL:
                return "the shell as a mouse wheel";
            case ARROW_KEY:
                return "the shell as arrow keys";
            case LOCAL_SCROLLBACK:
                return "the view's own scrollback";
        }
        throw new IllegalStateException("unhandled scroll gesture destination " + destination);
    }
}
