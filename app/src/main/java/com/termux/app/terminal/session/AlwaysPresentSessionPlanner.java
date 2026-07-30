package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AlwaysPresentSessionPlanner {

    /**
     * A session name the owner deleted is never planned again. The removal is held per session name,
     * so the always-present name list the owner typed in the settings screen stays exactly as typed.
     */
    @NonNull
    public List<String> planMissingSessionNames(@NonNull Collection<String> alwaysPresentSessionNames,
                                                @NonNull Collection<String> liveSessionNames,
                                                @NonNull Set<String> hiddenSessionNames,
                                                @NonNull Set<String> userRemovedSessionNames) {
        Set<String> liveNames = new LinkedHashSet<>(liveSessionNames);
        Set<String> planned = new LinkedHashSet<>();
        List<String> missingSessionNames = new ArrayList<>();
        for (String alwaysPresentSessionName : alwaysPresentSessionNames) {
            if (alwaysPresentSessionName == null) continue;
            String trimmedName = alwaysPresentSessionName.trim();
            if (trimmedName.isEmpty()) continue;
            if (userRemovedSessionNames.contains(trimmedName)) continue;
            if (HiddenSessionNameMatcher.matchesAHiddenSession(trimmedName, hiddenSessionNames)) continue;
            if (liveNames.contains(trimmedName)) continue;
            if (!planned.add(trimmedName)) continue;
            missingSessionNames.add(trimmedName);
        }
        return missingSessionNames;
    }
}
