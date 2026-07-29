package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * Decides whether the startup eager-load pass may materialize a session's emulator, which is what
 * spawns its process. A hidden session stays in the list and starts normally when the user opens it,
 * but it must not be started by the automatic startup pass.
 */
public final class HiddenSessionEagerLoadExclusion {

    private HiddenSessionEagerLoadExclusion() {
    }

    public static boolean shouldEagerLoadSession(@Nullable String sessionName,
                                                 @NonNull Set<String> hiddenSessionNames) {
        return sessionName == null || !hiddenSessionNames.contains(sessionName);
    }
}
