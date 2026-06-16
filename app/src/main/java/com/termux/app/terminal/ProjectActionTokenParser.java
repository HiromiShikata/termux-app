package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProjectActionTokenParser {

    private static final String TOKEN_SEPARATOR = ",";
    private static final String ACTION_SEPARATOR = ":";

    private ProjectActionTokenParser() {
    }

    @NonNull
    public static List<ProjectActionToken> parse(@Nullable String commaSeparatedTokens) {
        Set<ProjectActionToken> tokens = new LinkedHashSet<>();
        if (commaSeparatedTokens != null) {
            for (String rawToken : commaSeparatedTokens.split(TOKEN_SEPARATOR, -1)) {
                ProjectActionToken token = parseToken(rawToken);
                if (token != null) {
                    tokens.add(token);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    @Nullable
    private static ProjectActionToken parseToken(@NonNull String rawToken) {
        int actionSeparatorIndex = rawToken.indexOf(ACTION_SEPARATOR);
        if (actionSeparatorIndex < 0) {
            return null;
        }
        String rawProjectName = rawToken.substring(0, actionSeparatorIndex);
        String rawActionName = rawToken.substring(actionSeparatorIndex + ACTION_SEPARATOR.length());
        String normalizedProjectName = ExpandedProjectsAllowlistParser.normalize(rawProjectName);
        if (normalizedProjectName.isEmpty()) {
            return null;
        }
        ProjectAction action = ProjectAction.fromTokenName(rawActionName);
        if (action == null) {
            return null;
        }
        return new ProjectActionToken(normalizedProjectName, action);
    }
}
