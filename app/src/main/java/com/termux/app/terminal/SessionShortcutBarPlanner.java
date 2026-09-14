package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionShortcutBarPlanner {

    @NonNull
    public List<SessionShortcut> planRightToLeftShortcuts(@NonNull Set<String> alwaysNaSessionNames,
                                                          @NonNull List<SessionDefinitionEntry> entries) {
        return planRightToLeftShortcuts(alwaysNaSessionNames, entries, Collections.emptyList());
    }

    @NonNull
    public List<SessionShortcut> planRightToLeftShortcuts(@NonNull Set<String> alwaysNaSessionNames,
                                                          @NonNull List<SessionDefinitionEntry> entries,
                                                          @NonNull List<String> liveSessionNames) {
        return new ArrayList<>(planRightToLeftShortcutRows(alwaysNaSessionNames, entries,
            liveSessionNames).getAlwaysSessionShortcuts());
    }

    @NonNull
    public SessionShortcutRows planRightToLeftShortcutRows(
            @NonNull Set<String> alwaysNaSessionNames,
            @NonNull List<SessionDefinitionEntry> entries,
            @NonNull List<String> liveSessionNames) {
        List<SessionShortcut> alwaysSessionShortcuts = new ArrayList<>();
        Set<String> seenAlwaysNaTargets = new LinkedHashSet<>();
        Set<String> configuredNames = trimmedNonEmptyNames(alwaysNaSessionNames);
        for (String trimmedName : configuredNames) {
            String targetSessionName = resolveAlwaysNaTargetSessionName(trimmedName, entries,
                liveSessionNames, configuredNames);
            if (!seenAlwaysNaTargets.add(targetSessionName)) {
                continue;
            }
            alwaysSessionShortcuts.add(new SessionShortcut(trimmedName, targetSessionName));
        }
        return new SessionShortcutRows(alwaysSessionShortcuts);
    }

    @NonNull
    private static Set<String> trimmedNonEmptyNames(@NonNull Set<String> alwaysNaSessionNames) {
        Set<String> trimmedNames = new LinkedHashSet<>();
        for (String alwaysNaSessionName : alwaysNaSessionNames) {
            String trimmedName = alwaysNaSessionName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }
            trimmedNames.add(trimmedName);
        }
        return trimmedNames;
    }

    @NonNull
    private String resolveAlwaysNaTargetSessionName(@NonNull String configuredName,
                                                    @NonNull List<SessionDefinitionEntry> entries,
                                                    @NonNull List<String> liveSessionNames,
                                                    @NonNull Set<String> configuredNames) {
        if (liveSessionNames.contains(configuredName)) {
            return configuredName;
        }
        String liveSessionNameForCompositeEntry =
            liveSessionNameForCompositeEntry(configuredName, entries, liveSessionNames);
        if (liveSessionNameForCompositeEntry != null
                && !configuredNames.contains(liveSessionNameForCompositeEntry)) {
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
