package com.termux.app.sessiondefinition;

import android.view.View;
import android.widget.ProgressBar;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionController {

    private static final String LOG_TAG = "SessionDefinitionController";

    private final TermuxActivity activity;
    private final SessionDefinitionLoader loader;
    private final SessionDefinitionPlanner planner;

    public SessionDefinitionController(TermuxActivity activity) {
        this(activity, new SessionDefinitionLoader(new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser()), new SessionDefinitionPlanner());
    }

    public SessionDefinitionController(TermuxActivity activity, SessionDefinitionLoader loader, SessionDefinitionPlanner planner) {
        this.activity = activity;
        this.loader = loader;
        this.planner = planner;
    }

    public void loadAndBuildSessions() {
        String baseUrl = activity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            activity.showToast(activity.getString(R.string.msg_session_definition_url_not_set), true);
            return;
        }

        setLoadingProgressVisible(true);

        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> entries = loader.load(baseUrl);
                activity.runOnUiThread(() -> {
                    try {
                        buildSessions(entries);
                    } finally {
                        setLoadingProgressVisible(false);
                    }
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition from " + baseUrl, exception);
                activity.runOnUiThread(() -> {
                    try {
                        activity.showToast(activity.getString(R.string.msg_session_definition_load_failed), true);
                    } finally {
                        setLoadingProgressVisible(false);
                    }
                });
            }
        }).start();
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
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, collectExistingSessionNames());

        for (SessionDefinitionPlannedSession plannedSession : sessionsToCreate) {
            if (plannedSession.hasCommand()) {
                activity.getTermuxTerminalSessionClient().addNewAutosshSession(plannedSession.getName(), plannedSession.getCommand());
            } else {
                activity.getTermuxTerminalSessionClient().addNewSession(false, plannedSession.getName());
            }
        }

        activity.getDrawer().closeDrawers();
    }

    private Set<String> collectExistingSessionNames() {
        Set<String> existingSessionNames = new HashSet<>();
        TermuxService service = activity.getTermuxService();
        if (service == null) {
            return existingSessionNames;
        }
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            existingSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        return existingSessionNames;
    }
}
