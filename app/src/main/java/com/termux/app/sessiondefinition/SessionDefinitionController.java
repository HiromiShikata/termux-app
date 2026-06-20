package com.termux.app.sessiondefinition;

import android.view.View;
import android.widget.ProgressBar;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        repository.loadForRebuild(baseUrl, entries -> {
            try {
                buildSessions(entries);
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
            return;
        }

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, Collections.emptySet());

        removeProjectLinkedSessions(entries);

        for (SessionDefinitionPlannedSession plannedSession : sessionsToCreate) {
            if (plannedSession.hasCommand()) {
                activity.getTermuxTerminalSessionClient().addNewAutosshSession(plannedSession.getName(), plannedSession.getCommand(), false);
            } else {
                activity.getTermuxTerminalSessionClient().addNewSession(false, plannedSession.getName(), false);
            }
        }

        activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions();

        activity.getTermuxTerminalSessionClient().ensureCurrentSessionValidAfterRebuild();
    }

    private void removeProjectLinkedSessions(List<SessionDefinitionEntry> entries) {
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return;
        }
        List<TerminalSession> projectLinkedSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) {
                continue;
            }
            if (matcher.findEntryForSessionName(entries, terminalSession.mSessionName) != null) {
                projectLinkedSessions.add(terminalSession);
            }
        }
        for (TerminalSession terminalSession : projectLinkedSessions) {
            activity.getTermuxTerminalSessionClient().removeSessionForRebuild(terminalSession);
        }
    }
}
