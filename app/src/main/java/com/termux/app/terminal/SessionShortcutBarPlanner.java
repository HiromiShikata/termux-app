package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import java.util.ArrayList;
import java.util.Collections;
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
        return planRightToLeftShortcuts(alwaysNaSessionNames, entries, Collections.emptyList());
    }

    @NonNull
    public List<SessionShortcut> planRightToLeftShortcuts(@NonNull Set<String> alwaysNaSessionNames,
                                                          @NonNull List<SessionDefinitionEntry> entries,
                                                          @NonNull List<String> liveSessionNames) {
        List<SessionShortcut> rightToLeftShortcuts = new ArrayList<>();
        Set<String> seenAlwaysNaTargets = new LinkedHashSet<>();
        for (String alwaysNaSessionName : alwaysNaSessionNames) {
            String trimmedName = alwaysNaSessionName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }
            String targetSessionName =
                resolveAlwaysNaTargetSessionName(trimmedName, entries, liveSessionNames);
            if (!seenAlwaysNaTargets.add(targetSessionName)) {
                continue;
            }
            rightToLeftShortcuts.add(new SessionShortcut(trimmedName, targetSessionName));
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
    private String resolveAlwaysNaTargetSessionName(@NonNull String configuredName,
                                                    @NonNull List<SessionDefinitionEntry> entries,
                                                    @NonNull List<String> liveSessionNames) {
        if (liveSessionNames.contains(configuredName)) {
            return configuredName;
        }
        String liveSessionNameForCompositeEntry =
            liveSessionNameForCompositeEntry(configuredName, entries, liveSessionNames);
        if (liveSessionNameForCompositeEntry != null) {
            return liveSessionNameForCompositeEntry;
        }
        return configuredName;
    }

    @Nullable
    private String liveSessionNameForCompositeEntry(@NonNull String configuredName,
                                                    @NonNull List<SessionDefinitionEntry> entries,
                                                    @NonNull List<String> liveSessionNames) {
        for (SessionDefinitionEntry entry : entries) {
            if (!configuredName.equals(entry.getSessionName())) {
                continue;
            }
            String singleLiveUrl = null;
            for (String url : entry.getUrls()) {
                if (!liveSessionNames.contains(url)) {
                    continue;
                }
                if (singleLiveUrl != null) {
                    return null;
                }
                singleLiveUrl = url;
            }
            return singleLiveUrl;
        }
        return null;
    }

    @NonNull
    public static List<SessionShortcut> renderOrderShortcuts(
            @NonNull List<SessionShortcut> rightToLeftShortcuts) {
        List<SessionShortcut> renderOrderShortcuts = new ArrayList<>();
        for (int index = rightToLeftShortcuts.size() - 1; index >= 0; index--) {
            renderOrderShortcuts.add(rightToLeftShortcuts.get(index));
        }
        return renderOrderShortcuts;
    }
}
