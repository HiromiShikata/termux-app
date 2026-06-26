package com.termux.app.sessiondefinition;

import android.view.View;
import android.widget.ProgressBar;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionController {

    private static final String LOG_TAG = "SessionDefinitionController";

    private final TermuxActivity activity;
    private final SessionDefinitionRepository repository;
    private final SessionDefinitionPlanner planner;
    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    public SessionDefinitionController(TermuxActivity activity) {
        this(activity, new SessionDefinitionRepository(), new SessionDefinitionPlanner());
    }

    public SessionDefinitionController(TermuxActivity activity, SessionDefinitionRepository repository, SessionDefinitionPlanner planner) {
        this.activity = activity;
        this.repository = repository;
        this.planner = planner;
    }

    public void loadAndBuildSessions() {
        String baseUrl = activity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_url_not_set), true);
            return;
        }

        setLoadingProgressVisible(true);

        repository.loadForRebuild(baseUrl, result -> {
            try {
                notifyPartialLoad(result);
                buildSessions(result.getEntries());
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

    private void buildSessions(List<SessionDefinitionEntry> entries) {
        String commandTemplate = activity.getPreferences().getAutosshCommand();
        List<SessionDefinitionPlannedSession> plannedSessions = planner.plan(entries, commandTemplate);

        if (plannedSessions.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_no_entries), true);
        }

        Set<String> liveSessionNames = collectLiveSessionNames();

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, liveSessionNames);

        TerminalSession displayedSessionBeforeReload = activity.getCurrentSession();

        removeSessionsWithDisappearedDefinition(entries);

        int configuredLimit = activity.getPreferences().getSessionDefinitionMaxSessions();
        SessionDefinitionLimitPlan limitPlan =
            SessionDefinitionLimitPlan.forCapacity(sessionsToCreate.size(), liveSessionNames.size(), configuredLimit);

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

        activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions();

        activity.getTermuxTerminalSessionClient().ensureCurrentSessionValidAfterRebuild();

        activity.getTermuxTerminalSessionClient()
            .restoreDisplayedSessionAfterReloadIfStillLive(displayedSessionBeforeReload);
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

    private void removeSessionsWithDisappearedDefinition(List<SessionDefinitionEntry> currentEntries) {
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return;
        }
        List<SessionDefinitionEntry> previousEntries = previousDefinitionEntries();
        List<TerminalSession> sessionsToRemove = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) {
                continue;
            }
            String sessionName = terminalSession.mSessionName;
            boolean wasProjectLinked = matcher.findEntryForSessionName(previousEntries, sessionName) != null;
            boolean isStillDefined = matcher.findEntryForSessionName(currentEntries, sessionName) != null;
            if (wasProjectLinked && !isStillDefined) {
                sessionsToRemove.add(terminalSession);
            }
        }
        for (TerminalSession terminalSession : sessionsToRemove) {
            activity.getTermuxTerminalSessionClient().removeSessionForRebuild(terminalSession);
        }
    }

    private List<SessionDefinitionEntry> previousDefinitionEntries() {
        TermuxSessionsListViewController listViewController = activity.getTermuxSessionListViewController();
        if (listViewController == null) {
            return new ArrayList<>();
        }
        return listViewController.getEntries();
    }
}
