package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionPlannedSession;

import java.util.Collections;
import java.util.Set;

public final class FinishedSessionEnterAction {

    public enum Kind {
        RECONNECT,
        REMOVE
    }

    @NonNull
    private final Kind kind;

    @Nullable
    private final String sessionName;

    @Nullable
    private final String command;

    private FinishedSessionEnterAction(@NonNull Kind kind, @Nullable String sessionName, @Nullable String command) {
        this.kind = kind;
        this.sessionName = sessionName;
        this.command = command;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    public boolean isReconnect() {
        return kind == Kind.RECONNECT;
    }

    @Nullable
    public String getSessionName() {
        return sessionName;
    }

    @Nullable
    public String getCommand() {
        return command;
    }

    @NonNull
    public static FinishedSessionEnterAction decide(@Nullable String sessionName, @Nullable String autosshCommandTemplate) {
        return decide(sessionName, autosshCommandTemplate, Collections.emptySet());
    }

    @NonNull
    public static FinishedSessionEnterAction decide(@Nullable String sessionName, @Nullable String autosshCommandTemplate,
                                                    @NonNull Set<String> userRemovedSessionNames) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        if (userRemovedSessionNames.contains(sessionName)) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        SessionDefinitionPlannedSession plannedSession =
            new SessionDefinitionPlanner().planNamedSession(sessionName, autosshCommandTemplate);
        if (!plannedSession.hasCommand()) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        return new FinishedSessionEnterAction(Kind.RECONNECT, plannedSession.getName(), plannedSession.getCommand());
    }
}
