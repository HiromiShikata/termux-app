package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public final class UserRemovedSessionReconnectSuppressionPlanner {

    public boolean shouldSuppressReconnectAfterUserRemoval(@Nullable String sessionName,
                                                           @NonNull Set<String> alwaysPresentSessionNames) {
        if (sessionName == null) {
            return false;
        }
        String trimmedName = sessionName.trim();
        if (trimmedName.isEmpty()) {
            return false;
        }
        return !alwaysPresentSessionNames.contains(trimmedName);
    }
}
