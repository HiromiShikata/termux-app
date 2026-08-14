package com.termux.app.ownercall;

import android.view.View;

import androidx.annotation.NonNull;

public final class OwnerCallDialogRelayoutWatcher {

    private OwnerCallDialogRelayoutWatcher() {
    }

    public static void watchTerminalArea(@NonNull View terminalArea,
                                         @NonNull Runnable onTerminalAreaBoundsChanged) {
        terminalArea.addOnLayoutChangeListener(
            (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
                    return;
                }
                onTerminalAreaBoundsChanged.run();
            });
    }
}
