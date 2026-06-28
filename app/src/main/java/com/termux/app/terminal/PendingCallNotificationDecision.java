package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PendingCallNotificationDecision {

    private final Set<String> newlyPendingSessionNames;
    private final Set<String> nextNotifiedSessionNames;

    private PendingCallNotificationDecision(@NonNull Set<String> newlyPendingSessionNames,
                                           @NonNull Set<String> nextNotifiedSessionNames) {
        this.newlyPendingSessionNames =
            Collections.unmodifiableSet(new LinkedHashSet<>(newlyPendingSessionNames));
        this.nextNotifiedSessionNames =
            Collections.unmodifiableSet(new HashSet<>(nextNotifiedSessionNames));
    }

    /**
     * Decides which sessions should fire a real-time heads-up call-to-user notification right now,
     * given the authoritative set of sessions currently pending an un-replied call and the set of
     * sessions a heads-up was already fired for. A session fires exactly once on the transition into
     * the pending set: a session present in {@code currentlyPendingSessionNames} but absent from
     * {@code alreadyNotifiedSessionNames} is newly pending and fires; a session in both is the same
     * un-replied call and is suppressed. The returned {@link #nextNotifiedSessionNames} is exactly
     * the current pending set, so a session that caught up (replied) and dropped out of the pending
     * set is removed from the tracked set and will fire again on its next new call.
     */
    @NonNull
    public static PendingCallNotificationDecision decide(
            @NonNull Set<String> currentlyPendingSessionNames,
            @NonNull Set<String> alreadyNotifiedSessionNames) {
        Set<String> newlyPendingSessionNames = new LinkedHashSet<>(currentlyPendingSessionNames);
        newlyPendingSessionNames.removeAll(alreadyNotifiedSessionNames);
        return new PendingCallNotificationDecision(newlyPendingSessionNames,
            currentlyPendingSessionNames);
    }

    @NonNull
    public Set<String> getNewlyPendingSessionNames() {
        return newlyPendingSessionNames;
    }

    @NonNull
    public Set<String> getNextNotifiedSessionNames() {
        return nextNotifiedSessionNames;
    }
}
