package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import java.util.List;

public final class SessionEagerLoader {

    public interface SessionListSupplier {
        @NonNull
        List<TerminalSession> getSessionsToEagerLoad();
    }

    public interface SessionInitializationState {
        boolean isSessionInitialized(@NonNull TerminalSession session);
    }

    public interface SessionInitializationAction {
        void initializeSession(@NonNull TerminalSession session);
    }

    private final SessionListSupplier sessionListSupplier;

    private final SessionInitializationState sessionInitializationState;

    private final SessionInitializationAction sessionInitializationAction;

    public SessionEagerLoader(
        @NonNull SessionListSupplier sessionListSupplier,
        @NonNull SessionInitializationState sessionInitializationState,
        @NonNull SessionInitializationAction sessionInitializationAction) {
        this.sessionListSupplier = sessionListSupplier;
        this.sessionInitializationState = sessionInitializationState;
        this.sessionInitializationAction = sessionInitializationAction;
    }

    public void eagerLoadAllSessions() {
        for (TerminalSession session : sessionListSupplier.getSessionsToEagerLoad()) {
            if (session == null) continue;
            if (sessionInitializationState.isSessionInitialized(session)) continue;
            sessionInitializationAction.initializeSession(session);
        }
    }
}
