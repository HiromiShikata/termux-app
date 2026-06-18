package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionDefinitionPlannedSession;
import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class AlwaysPresentSessionStartupPlanner {

    @NonNull
    private final SessionDefinitionPlanner sessionDefinitionPlanner = new SessionDefinitionPlanner();

    @NonNull
    public AlwaysPresentSessionStartup planStartup(@NonNull String sessionName, String commandTemplate,
                                                   @NonNull String shellPath) {
        SessionDefinitionPlannedSession plannedSession =
            sessionDefinitionPlanner.planNamedSession(sessionName, commandTemplate);
        if (!plannedSession.hasCommand()) {
            return new AlwaysPresentSessionStartup(sessionName, null, null);
        }
        return new AlwaysPresentSessionStartup(sessionName, shellPath,
            new String[]{"-c", plannedSession.getCommand()});
    }
}
