package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public final class HiddenSessionEagerLoadExclusion {

    private HiddenSessionEagerLoadExclusion() {
    }

    public static boolean shouldEagerLoadSession(@Nullable String sessionName,
                                                 @NonNull Set<String> hiddenSessionNames) {
        return sessionName == null || !hiddenSessionNames.contains(sessionName);
    }
}
