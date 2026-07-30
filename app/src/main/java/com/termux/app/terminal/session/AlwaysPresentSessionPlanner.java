package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AlwaysPresentSessionPlanner {

    @NonNull
    public List<String> planMissingSessionNames(@NonNull Collection<String> alwaysPresentSessionNames,
                                                @NonNull Collection<String> liveSessionNames,
                                                @NonNull Set<String> hiddenSessionNames) {
        Set<String> liveNames = new LinkedHashSet<>(liveSessionNames);
        Set<String> planned = new LinkedHashSet<>();
        List<String> missingSessionNames = new ArrayList<>();
        for (String alwaysPresentSessionName : alwaysPresentSessionNames) {
            if (alwaysPresentSessionName == null) continue;
            String trimmedName = alwaysPresentSessionName.trim();
            if (trimmedName.isEmpty()) continue;
            if (HiddenSessionNameMatcher.matchesAHiddenSession(trimmedName, hiddenSessionNames)) continue;
            if (liveNames.contains(trimmedName)) continue;
            if (!planned.add(trimmedName)) continue;
            missingSessionNames.add(trimmedName);
        }
        return missingSessionNames;
    }
}
