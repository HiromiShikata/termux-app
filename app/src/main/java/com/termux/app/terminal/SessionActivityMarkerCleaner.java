package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionActivityMarkerCleaner {

    private SessionActivityMarkerCleaner() {
    }

    public static boolean clearActivityMarkers(@NonNull SessionBellNotificationStore bellNotificationStore,
                                               @NonNull SessionOutputActivityStore outputActivityStore,
                                               @Nullable String sessionHandle) {
        if (sessionHandle == null) {
            return false;
        }
        boolean hadBellNotification = bellNotificationStore.hasPendingNotification(sessionHandle);
        boolean hadOutputActivity = outputActivityStore.hasOutputActivity(sessionHandle);
        bellNotificationStore.clearBell(sessionHandle);
        SessionOutputActivityMarker.clearOutputActivityForCurrentSession(outputActivityStore, sessionHandle);
        return hadBellNotification || hadOutputActivity;
    }
}
