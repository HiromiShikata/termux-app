package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ExpandedProjectsAllowlistParser {

    private static final String TOKEN_SEPARATOR = ",";
    private static final String ACTION_SEPARATOR = ":";

    private ExpandedProjectsAllowlistParser() {
    }

    @NonNull
    public static List<String> parse(@Nullable String commaSeparatedProjects) {
        Set<String> tokens = new LinkedHashSet<>();
        if (commaSeparatedProjects != null) {
            for (String rawToken : commaSeparatedProjects.split(TOKEN_SEPARATOR, -1)) {
                String normalizedToken = normalize(stripActionSuffix(rawToken));
                if (!normalizedToken.isEmpty()) {
                    tokens.add(normalizedToken);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    @NonNull
    private static String stripActionSuffix(@NonNull String rawToken) {
        int actionSeparatorIndex = rawToken.indexOf(ACTION_SEPARATOR);
        if (actionSeparatorIndex < 0) {
            return rawToken;
        }
        return rawToken.substring(0, actionSeparatorIndex);
    }

    @NonNull
    public static String normalize(@NonNull String rawToken) {
        return rawToken.trim().toLowerCase(Locale.ROOT);
    }
}
