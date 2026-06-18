package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DuplicateSessionNameResolution {

    private final boolean revealExisting;
    private final String existingSessionName;
    private final boolean requiresUnhide;

    private DuplicateSessionNameResolution(boolean revealExisting, @Nullable String existingSessionName,
                                           boolean requiresUnhide) {
        this.revealExisting = revealExisting;
        this.existingSessionName = existingSessionName;
        this.requiresUnhide = requiresUnhide;
    }

    @NonNull
    public static DuplicateSessionNameResolution create() {
        return new DuplicateSessionNameResolution(false, null, false);
    }

    @NonNull
    public static DuplicateSessionNameResolution reveal(@NonNull String existingSessionName, boolean requiresUnhide) {
        return new DuplicateSessionNameResolution(true, existingSessionName, requiresUnhide);
    }

    public boolean shouldRevealExisting() {
        return revealExisting;
    }

    @Nullable
    public String getExistingSessionName() {
        return existingSessionName;
    }

    public boolean requiresUnhide() {
        return requiresUnhide;
    }
}
