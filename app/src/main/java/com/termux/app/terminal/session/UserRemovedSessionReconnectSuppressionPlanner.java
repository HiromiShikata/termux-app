package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public final class UserRemovedSessionReconnectSuppressionPlanner {

    /**
     * A deletion is a deletion. The removal is recorded for every session name the owner deleted,
     * including a name that appears in the always-present name list, so that no restore path creates
     * that session again on its own. The always-present name list the owner typed in the settings
     * screen is never edited to achieve this; only the per-session removal record is written.
     */
    public boolean shouldSuppressReconnectAfterUserRemoval(@Nullable String sessionName) {
        return sessionName != null && !sessionName.trim().isEmpty();
    }

    /**
     * Killing the host session tears the local session down so that it can be established again, so it
     * is not a removal. An always-present session name therefore keeps being created automatically
     * after a host session kill, exactly as it did before.
     */
    public boolean shouldSuppressReconnectAfterHostSessionKill(
            @Nullable String sessionName, @NonNull Set<String> alwaysPresentSessionNames) {
        if (!shouldSuppressReconnectAfterUserRemoval(sessionName)) {
            return false;
        }
        return !alwaysPresentSessionNames.contains(sessionName.trim());
    }
}
