package com.termux.shared.termux.settings.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public final class HiddenSessionNameMatcher {

    private HiddenSessionNameMatcher() {
    }

    public static boolean matchesAHiddenSession(@Nullable String sessionName,
                                                @NonNull Set<String> hiddenSessionNames) {
        return sessionName != null && hiddenSessionNames.contains(sessionName);
    }
}
