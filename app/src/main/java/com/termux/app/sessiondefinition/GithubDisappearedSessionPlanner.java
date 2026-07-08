package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GithubDisappearedSessionPlanner {

    private final DefaultProjectManagerSessionPlanner defaultProjectManagerSessionPlanner =
        new DefaultProjectManagerSessionPlanner();

    @NonNull
    public Set<String> collectSessionNamesInList(@NonNull List<SessionDefinitionEntry> currentEntries) {
        Set<String> namesInList = new HashSet<>();
        for (SessionDefinitionEntry entry : currentEntries) {
            namesInList.addAll(entry.getUrls());
        }
        namesInList.addAll(defaultProjectManagerSessionPlanner.planSessionNames(currentEntries));
        return namesInList;
    }

    @NonNull
    public List<String> planGithubSessionNamesToRemove(@NonNull List<SessionDefinitionEntry> currentEntries,
                                                       @NonNull Set<String> protectedSessionNames,
                                                       @NonNull List<String> liveSessionNames,
                                                       boolean removeGithubSessionsNotInList) {
        if (!removeGithubSessionsNotInList) {
            return Collections.emptyList();
        }
        Set<String> namesInList = collectSessionNamesInList(currentEntries);
        List<String> sessionNamesToRemove = new ArrayList<>();
        for (String sessionName : liveSessionNames) {
            if (!GithubSessionName.isGithubSessionName(sessionName)) {
                continue;
            }
            if (namesInList.contains(sessionName) || protectedSessionNames.contains(sessionName)) {
                continue;
            }
            sessionNamesToRemove.add(sessionName);
        }
        return sessionNamesToRemove;
    }

    @NonNull
    public List<String> planGithubHiddenSessionNamesToPrune(@NonNull List<SessionDefinitionEntry> currentEntries,
                                                           @NonNull Set<String> persistedHiddenSessionNames) {
        Set<String> namesInList = collectSessionNamesInList(currentEntries);
        List<String> hiddenSessionNamesToPrune = new ArrayList<>();
        for (String sessionName : persistedHiddenSessionNames) {
            if (!GithubSessionName.isGithubSessionName(sessionName)) {
                continue;
            }
            if (namesInList.contains(sessionName)) {
                continue;
            }
            hiddenSessionNamesToPrune.add(sessionName);
        }
        return hiddenSessionNamesToPrune;
    }
}
