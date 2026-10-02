package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.GithubSessionName;
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
        return decide(sessionName, autosshCommandTemplate, userRemovedSessionNames, false,
            Collections.emptySet(), Collections.emptySet(), true);
    }

    /**
     * Decides whether a finished session should be reconnected or removed. A GitHub-URL-named session
     * ({@code sessionName} starting with {@code https://github.com/}) is additionally removed, ahead of
     * the usual session-definition planning, once the published session list has been fetched
     * successfully at least once ({@code publishedSessionListLoaded}) and the "remove GitHub sessions
     * not in list" preference is enabled ({@code removeGithubSessionsNotInList}), when that session's
     * name is absent from both the most recently cached published list ({@code publishedSessionNames})
     * and the always-present session names ({@code alwaysPresentSessionNames}). This keeps a session the
     * owner's Reactivation Trigger already excluded from the published list from being recreated by any
     * reconnect path.
     */
    @NonNull
    public static FinishedSessionEnterAction decide(@Nullable String sessionName, @Nullable String autosshCommandTemplate,
                                                    @NonNull Set<String> userRemovedSessionNames,
                                                    boolean publishedSessionListLoaded,
                                                    @NonNull Set<String> publishedSessionNames,
                                                    @NonNull Set<String> alwaysPresentSessionNames,
                                                    boolean removeGithubSessionsNotInList) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        if (TransientCommandSessionName.isTransient(sessionName)) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        if (userRemovedSessionNames.contains(sessionName)) {
            return new FinishedSessionEnterAction(Kind.REMOVE, sessionName, null);
        }
        if (GithubSessionName.isGithubSessionName(sessionName)
            && publishedSessionListLoaded
            && removeGithubSessionsNotInList
            && !alwaysPresentSessionNames.contains(sessionName)
            && !publishedSessionNames.contains(sessionName)) {
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
