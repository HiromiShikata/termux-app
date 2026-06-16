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

    private ExpandedProjectsAllowlistParser() {
    }

    @NonNull
    public static List<String> parse(@Nullable String commaSeparatedProjects) {
        Set<String> tokens = new LinkedHashSet<>();
        if (commaSeparatedProjects != null) {
            for (String rawToken : commaSeparatedProjects.split(TOKEN_SEPARATOR, -1)) {
                String normalizedToken = normalize(rawToken);
                if (!normalizedToken.isEmpty()) {
                    tokens.add(normalizedToken);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    @NonNull
    public static String normalize(@NonNull String rawToken) {
        return rawToken.trim().toLowerCase(Locale.ROOT);
    }
}
