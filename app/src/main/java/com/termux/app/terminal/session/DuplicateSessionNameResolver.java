package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.List;
import java.util.Set;

public final class DuplicateSessionNameResolver {

    private DuplicateSessionNameResolver() {
    }

    @NonNull
    public static DuplicateSessionNameResolution resolve(@Nullable String candidateName,
                                                         @NonNull List<String> existingSessionNames,
                                                         @NonNull Set<String> hiddenSessionNames) {
        if (candidateName == null || candidateName.isEmpty()) {
            return DuplicateSessionNameResolution.create();
        }
        for (String existingSessionName : existingSessionNames) {
            if (candidateName.equals(existingSessionName)) {
                return DuplicateSessionNameResolution.reveal(existingSessionName,
                    HiddenSessionNameMatcher.matchesAHiddenSession(existingSessionName, hiddenSessionNames));
            }
        }
        return DuplicateSessionNameResolution.create();
    }
}
