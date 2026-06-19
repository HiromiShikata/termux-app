package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionOutputActivityMarker {

    private SessionOutputActivityMarker() {
    }

    public static boolean markBackgroundOutputActivity(@NonNull SessionOutputActivityStore store,
                                                       @Nullable String currentSessionHandle,
                                                       @Nullable String changedSessionHandle) {
        if (changedSessionHandle == null) {
            return false;
        }
        if (currentSessionHandle == null) {
            return false;
        }
        if (changedSessionHandle.equals(currentSessionHandle)) {
            return false;
        }
        store.markOutputActivity(changedSessionHandle);
        return true;
    }

    public static void clearOutputActivityForCurrentSession(@NonNull SessionOutputActivityStore store,
                                                            @Nullable String currentSessionHandle) {
        if (currentSessionHandle == null) {
            return;
        }
        store.clearOutputActivity(currentSessionHandle);
    }
}
