package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public final class CallToUserAlertCancellation {

    private CallToUserAlertCancellation() {
    }

    @NonNull
    public static Set<String> sessionsToCancel(@NonNull Set<String> knownAlertSessionNames,
                                               @NonNull Set<String> pendingSessionNames) {
        Set<String> sessionNames = new HashSet<>(knownAlertSessionNames);
        sessionNames.removeAll(pendingSessionNames);
        return sessionNames;
    }
}
