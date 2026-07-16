package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionShortcutBarPlanner {

    private final DefaultProjectManagerSessionPlanner projectManagerSessionPlanner;

    public SessionShortcutBarPlanner(@NonNull DefaultProjectManagerSessionPlanner projectManagerSessionPlanner) {
        this.projectManagerSessionPlanner = projectManagerSessionPlanner;
    }

    @NonNull
    public List<SessionShortcut> planRightToLeftShortcuts(@NonNull Set<String> alwaysNaSessionNames,
                                                          @NonNull List<SessionDefinitionEntry> entries) {
        List<SessionShortcut> rightToLeftShortcuts = new ArrayList<>();
        for (String alwaysNaSessionName : alwaysNaSessionNames) {
            String trimmedName = alwaysNaSessionName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }
            rightToLeftShortcuts.add(new SessionShortcut(trimmedName, trimmedName));
        }
        Set<String> seenProjectLabels = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            String projectLabel = entry.getGroupLabel();
            String pmSessionName = projectManagerSessionPlanner.sessionNameForProjectLabel(projectLabel);
            if (pmSessionName == null) {
                continue;
            }
            if (!seenProjectLabels.add(projectLabel)) {
                continue;
            }
            rightToLeftShortcuts.add(new SessionShortcut(projectLabel.trim(), pmSessionName));
        }
        return rightToLeftShortcuts;
    }

    @NonNull
    public static List<SessionShortcut> renderOrderPresentShortcuts(
            @NonNull List<SessionShortcut> rightToLeftShortcuts,
            @NonNull Set<String> presentSessionNames) {
        List<SessionShortcut> renderOrderShortcuts = new ArrayList<>();
        for (int index = rightToLeftShortcuts.size() - 1; index >= 0; index--) {
            SessionShortcut shortcut = rightToLeftShortcuts.get(index);
            if (!presentSessionNames.contains(shortcut.getTargetSessionName())) {
                continue;
            }
            renderOrderShortcuts.add(shortcut);
        }
        return renderOrderShortcuts;
    }
}
