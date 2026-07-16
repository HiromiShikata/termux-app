package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionShortcutBarPlanner {

    private final DefaultProjectManagerSessionPlanner projectManagerSessionPlanner;
    private final SessionDefinitionEntryMatcher sessionDefinitionEntryMatcher;

    public SessionShortcutBarPlanner(@NonNull DefaultProjectManagerSessionPlanner projectManagerSessionPlanner) {
        this(projectManagerSessionPlanner, new SessionDefinitionEntryMatcher());
    }

    public SessionShortcutBarPlanner(@NonNull DefaultProjectManagerSessionPlanner projectManagerSessionPlanner,
                                     @NonNull SessionDefinitionEntryMatcher sessionDefinitionEntryMatcher) {
        this.projectManagerSessionPlanner = projectManagerSessionPlanner;
        this.sessionDefinitionEntryMatcher = sessionDefinitionEntryMatcher;
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
        Set<String> projectManagerSessionNames = projectManagerSessionNames(entries);
        Set<String> seenAlwaysNaTargets = new LinkedHashSet<>();
        for (String alwaysNaSessionName : alwaysNaSessionNames) {
            String trimmedName = alwaysNaSessionName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }
            String targetSessionName = resolveAlwaysNaTargetSessionName(
                trimmedName, entries, liveSessionNames, projectManagerSessionNames);
            if (projectManagerSessionNames.contains(targetSessionName)) {
                continue;
            }
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
                                                    @NonNull List<String> liveSessionNames,
                                                    @NonNull Set<String> projectManagerSessionNames) {
        if (liveSessionNames.contains(configuredName)) {
            return configuredName;
        }
        String liveSessionNameForCompositeEntry =
            liveSessionNameForCompositeEntry(configuredName, entries, liveSessionNames);
        if (liveSessionNameForCompositeEntry != null) {
            return liveSessionNameForCompositeEntry;
        }
        String liveSessionNameByResolvedName = uniqueLiveSessionNameByResolvedName(
            configuredName, entries, liveSessionNames, projectManagerSessionNames);
        if (liveSessionNameByResolvedName != null) {
            return liveSessionNameByResolvedName;
        }
        return configuredName;
    }

    @Nullable
    private String uniqueLiveSessionNameByResolvedName(@NonNull String configuredName,
                                                       @NonNull List<SessionDefinitionEntry> entries,
                                                       @NonNull List<String> liveSessionNames,
                                                       @NonNull Set<String> projectManagerSessionNames) {
        String uniqueLiveSessionName = null;
        for (String liveSessionName : liveSessionNames) {
            if (liveSessionName == null || liveSessionName.isEmpty()) {
                continue;
            }
            if (projectManagerSessionNames.contains(liveSessionName)) {
                continue;
            }
            if (!liveSessionMatchesConfiguredName(liveSessionName, configuredName, entries)) {
                continue;
            }
            if (uniqueLiveSessionName != null) {
                return null;
            }
            uniqueLiveSessionName = liveSessionName;
        }
        return uniqueLiveSessionName;
    }

    private boolean liveSessionMatchesConfiguredName(@NonNull String liveSessionName,
                                                     @NonNull String configuredName,
                                                     @NonNull List<SessionDefinitionEntry> entries) {
        if (configuredName.equals(liveSessionName)) {
            return true;
        }
        if (configuredName.equals(
                sessionDefinitionEntryMatcher.findGroupLabelForSessionName(entries, liveSessionName))) {
            return true;
        }
        if (configuredName.equals(
                sessionDefinitionEntryMatcher.findTitleForSessionName(entries, liveSessionName))) {
            return true;
        }
        SessionDefinitionEntry owningEntry =
            sessionDefinitionEntryMatcher.findEntryForSessionName(entries, liveSessionName);
        return owningEntry != null && configuredName.equals(owningEntry.getSessionName());
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
    private Set<String> projectManagerSessionNames(@NonNull List<SessionDefinitionEntry> entries) {
        Set<String> projectManagerSessionNames = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            String pmSessionName = projectManagerSessionPlanner.sessionNameForProjectLabel(entry.getGroupLabel());
            if (pmSessionName != null) {
                projectManagerSessionNames.add(pmSessionName);
            }
        }
        return projectManagerSessionNames;
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
