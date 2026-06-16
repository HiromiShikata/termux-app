package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum ProjectAction {

    OVERVIEW_URL("overviewurl"),
    TDPM_CONSOLE_URL("tdpmconsoleurl"),
    NEW_ISSUE_URL("newissueurl");

    private final String tokenName;

    ProjectAction(@NonNull String tokenName) {
        this.tokenName = tokenName;
    }

    @Nullable
    public static ProjectAction fromTokenName(@Nullable String rawActionName) {
        if (rawActionName == null) {
            return null;
        }
        String normalizedActionName = rawActionName.trim().toLowerCase(Locale.ROOT);
        for (ProjectAction projectAction : values()) {
            if (projectAction.tokenName.equals(normalizedActionName)) {
                return projectAction;
            }
        }
        return null;
    }
}
