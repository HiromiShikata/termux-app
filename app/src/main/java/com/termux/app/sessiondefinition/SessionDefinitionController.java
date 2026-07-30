package com.termux.app.sessiondefinition;

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SessionDefinitionController {

    private static final String LOG_TAG = "SessionDefinitionController";

    private final TermuxActivity activity;
    private final SessionDefinitionRepository repository;
    private final SessionDefinitionPlanner planner;
    private final SessionDefinitionDisappearedSessionPlanner disappearedSessionPlanner =
        new SessionDefinitionDisappearedSessionPlanner();
    private final GithubDisappearedSessionPlanner githubDisappearedSessionPlanner =
        new GithubDisappearedSessionPlanner();
    private final DefaultProjectManagerSessionPlanner defaultProjectManagerSessionPlanner =
        new DefaultProjectManagerSessionPlanner();
    private final SessionDefinitionDuplicateSessionPlanner duplicateSessionPlanner =
        new SessionDefinitionDuplicateSessionPlanner();
    private final SessionDefinitionCapCountPlanner capCountPlanner =
        new SessionDefinitionCapCountPlanner();
    private final SessionDefinitionAlwaysPresentPriorityPlanner alwaysPresentPriorityPlanner =
        new SessionDefinitionAlwaysPresentPriorityPlanner();

    public SessionDefinitionController(TermuxActivity activity) {
        this(activity, new SessionDefinitionRepository(), new SessionDefinitionPlanner());
    }

    public SessionDefinitionController(TermuxActivity activity, SessionDefinitionRepository repository, SessionDefinitionPlanner planner) {
        this.activity = activity;
        this.repository = repository;
        this.planner = planner;
    }

    public void loadAndBuildSessions() {
        loadAndBuildSessions(false);
    }

    public void loadAndBuildSessions(boolean forceRefresh) {
        String baseUrl = activity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_url_not_set), true);
            return;
        }

        setLoadingProgressVisible(true);

        repository.loadForRebuild(baseUrl, forceRefresh, result -> {
            try {
                notifyPartialLoad(result);
                buildSessions(result);
            } finally {
                setLoadingProgressVisible(false);
            }
        }, exception -> {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition from " + baseUrl, exception);
            try {
                activity.showToast(activity.getString(R.string.msg_session_definition_load_failed), true);
            } finally {
                setLoadingProgressVisible(false);
            }
        });
    }

    public void restoreProjectManagerSessionsOnColdStart() {
        List<SessionDefinitionEntry> cachedEntries = repository.getCachedEntries();
        if (!cachedEntries.isEmpty()) {
            restoreProjectManagerSessions(cachedEntries);
            return;
        }
        repository.load(activity.getPreferences().getSessionDefinitionUrl().trim(),
            () -> restoreProjectManagerSessions(repository.getCachedEntries()));
    }

    private void restoreProjectManagerSessions(@NonNull List<SessionDefinitionEntry> entries) {
        List<String> projectManagerSessionNames = defaultProjectManagerSessionPlanner.planSessionNames(entries);
        if (projectManagerSessionNames.isEmpty()) {
            return;
        }
        activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions(projectManagerSessionNames);
    }

    private void notifyPartialLoad(SessionDefinitionLoadResult result) {
        if (!result.hasFailedGroups()) {
            return;
        }
        activity.showToast(activity.getString(R.string.msg_session_definition_load_partial,
            result.getFailedGroupCount(), result.getTotalGroupCount()), true);
    }

    private void setLoadingProgressVisible(boolean visible) {
        ProgressBar progressBar = activity.getSessionDefinitionLoadingProgressBar();
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void buildSessions(SessionDefinitionLoadResult result) {
        List<SessionDefinitionEntry> entries = result.getEntries();
        boolean authoritativeLoad = !result.hasFailedGroups();
        String commandTemplate = activity.getPreferences().getAutosshCommand();
        List<SessionDefinitionPlannedSession> definitionSessions = planner.plan(entries, commandTemplate);
        List<SessionDefinitionPlannedSession> plannedSessions =
            alwaysPresentPriorityPlanner.planPrioritizedSessions(
                definitionSessions, alwaysPresentSessionNames(), commandTemplate);

        if (definitionSessions.isEmpty() && authoritativeLoad) {
            activity.showToast(activity.getString(R.string.msg_session_definition_no_entries), true);
        }

        reconcileDuplicateLiveSessions();

        Set<String> liveSessionNames = collectLiveSessionNames();

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, liveSessionNames, hiddenSessionNames());

        TerminalSession displayedSessionBeforeReload = activity.getCurrentSession();

        if (authoritativeLoad) {
            removeSessionsWithDisappearedDefinition(entries);
        }

        int configuredLimit = activity.getPreferences().getSessionDefinitionMaxSessions();
        SessionDefinitionLimitPlan limitPlan =
            SessionDefinitionLimitPlan.forCapacity(sessionsToCreate.size(), countedLiveSessionCount(), configuredLimit);

        int createdCount = 0;
        for (SessionDefinitionPlannedSession plannedSession : sessionsToCreate) {
            if (createdCount >= limitPlan.getSessionsToCreateCount()) {
                break;
            }
            if (plannedSession.hasCommand()) {
                activity.getTermuxTerminalSessionClient().addNewAutosshSession(plannedSession.getName(), plannedSession.getCommand(), false);
            } else {
                activity.getTermuxTerminalSessionClient().addNewSession(false, plannedSession.getName(), false);
            }
            createdCount++;
        }

        if (limitPlan.exceedsLimit()) {
            activity.showToast(activity.getString(R.string.msg_session_limit_exceeded,
                configuredLimit, limitPlan.getDroppedSessionCount()), true);
        }

        activity.getTermuxTerminalSessionClient()
            .restoreAlwaysPresentSessions(defaultProjectManagerSessionPlanner.planSessionNames(entries));

        activity.getTermuxTerminalSessionClient().ensureCurrentSessionValidAfterRebuild();

        activity.getTermuxTerminalSessionClient()
            .restoreDisplayedSessionAfterReloadIfStillLive(displayedSessionBeforeReload);

        if (authoritativeLoad) {
            activity.refreshDisplayedSessionDefinitionEntries(entries);
        }
    }

    private Set<String> collectLiveSessionNames() {
        Set<String> liveSessionNames = new HashSet<>();
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return liveSessionNames;
        }
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) {
                continue;
            }
            liveSessionNames.add(terminalSession.mSessionName);
        }
        return liveSessionNames;
    }

    private int countedLiveSessionCount() {
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return 0;
        }
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            countedSessions.add(new SessionDefinitionCapCountPlanner.CountedSession(
                terminalSession == null ? null : terminalSession.mSessionName,
                terminalSession != null && terminalSession.isRunning()));
        }
        return capCountPlanner.countSessionsTowardCap(countedSessions, hiddenSessionNames());
    }

    private Set<String> hiddenSessionNames() {
        return activity.getPreferences() == null
            ? Collections.emptySet() : activity.getPreferences().getDisabledSessionNames();
    }

    private void reconcileDuplicateLiveSessions() {
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return;
        }
        List<TerminalSession> terminalSessionsByIndex = new ArrayList<>();
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            terminalSessionsByIndex.add(terminalSession);
            liveSessions.add(new SessionDefinitionDuplicateSessionPlanner.LiveSession(
                terminalSession == null ? null : terminalSession.mSessionName,
                terminalSession != null && terminalSession.isRunning()));
        }
        List<Integer> duplicateIndexesToRemove =
            duplicateSessionPlanner.planDuplicateSessionIndexesToRemove(liveSessions);
        for (int index : duplicateIndexesToRemove) {
            TerminalSession terminalSession = terminalSessionsByIndex.get(index);
            if (terminalSession != null) {
                activity.getTermuxTerminalSessionClient().removeSessionForRebuild(terminalSession);
            }
        }
    }

    private void removeSessionsWithDisappearedDefinition(List<SessionDefinitionEntry> currentEntries) {
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return;
        }
        Map<String, TerminalSession> sessionByName = new HashMap<>();
        List<SessionDefinitionDisappearedSessionPlanner.LiveSession> liveSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) {
                continue;
            }
            String sessionName = terminalSession.mSessionName;
            sessionByName.put(sessionName, terminalSession);
            liveSessions.add(new SessionDefinitionDisappearedSessionPlanner.LiveSession(
                sessionName, terminalSession.isRunning()));
        }
        Set<String> protectedSessionNames = new HashSet<>(alwaysPresentSessionNames());
        protectedSessionNames.addAll(defaultProjectManagerSessionPlanner.planSessionNames(currentEntries));
        List<String> sessionNamesToRemove = new ArrayList<>(disappearedSessionPlanner.planSessionNamesToRemove(
            currentEntries, protectedSessionNames, liveSessions));
        sessionNamesToRemove.addAll(githubDisappearedSessionPlanner.planGithubSessionNamesToRemove(
            currentEntries, protectedSessionNames, new ArrayList<>(sessionByName.keySet()),
            shouldRemoveGithubSessionsNotInList()));
        for (String sessionName : sessionNamesToRemove) {
            TerminalSession terminalSession = sessionByName.get(sessionName);
            if (terminalSession != null) {
                activity.getTermuxTerminalSessionClient().removeSessionForRebuild(terminalSession);
            }
        }
    }

    private boolean shouldRemoveGithubSessionsNotInList() {
        return activity.getPreferences() != null
            && activity.getPreferences().shouldRemoveGithubSessionsNotInList();
    }

    private Set<String> alwaysPresentSessionNames() {
        if (activity.getPreferences() == null) {
            return Collections.emptySet();
        }
        return activity.getPreferences().getAlwaysNaSessionNames();
    }
}
