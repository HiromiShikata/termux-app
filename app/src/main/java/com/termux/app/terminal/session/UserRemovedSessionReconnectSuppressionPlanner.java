package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

public final class UserRemovedSessionReconnectSuppressionPlanner {

    /**
     * A deletion is a deletion. The removal is recorded for every session name the owner deleted,
     * including a name that appears in the always-present name list, so that no restore path creates
     * that session again on its own. The always-present name list the owner typed in the settings
     * screen is never edited to achieve this; only the per-session removal record is written.
     */
    public boolean shouldSuppressReconnectAfterUserRemoval(@Nullable String sessionName) {
        if (sessionName == null) {
            return false;
        }
        return !sessionName.trim().isEmpty();
    }
}
