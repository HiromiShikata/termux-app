package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

public final class ShortcutNavigationProjectExpander {

    private ShortcutNavigationProjectExpander() {
    }

    public static boolean expandCollapsedProjectForSession(@NonNull List<SessionHierarchyRow> rows,
                                                           @NonNull Set<String> collapsedProjectKeys,
                                                           @Nullable String sessionName) {
        String projectLabel = SessionHierarchyBuilder.projectLabelForSessionName(rows, sessionName);
        if (projectLabel == null) {
            return false;
        }
        return collapsedProjectKeys.remove(projectLabel);
    }
}
