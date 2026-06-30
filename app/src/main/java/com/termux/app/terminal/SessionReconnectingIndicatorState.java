package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionReconnectingIndicatorState {

    private SessionReconnectingIndicatorState() {
    }

    public static boolean shouldShowReconnectingIndicator(@NonNull String sessionName,
                                                          @Nullable SessionNewActivityStore store) {
        if (store == null) {
            return false;
        }
        return store.isReconnecting(sessionName);
    }
}
