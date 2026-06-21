package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SessionEagerLoaderTest {

    private static TerminalSession newSession() {
        return new TerminalSession("/bin/sh", "/", new String[0], new String[0], null, null);
    }

    private static final class RecordingSessionList implements SessionEagerLoader.SessionListSupplier {
        private final List<TerminalSession> sessions;

        RecordingSessionList(List<TerminalSession> sessions) {
            this.sessions = sessions;
        }

        @NonNull
        @Override
        public List<TerminalSession> getSessionsToEagerLoad() {
            return sessions;
        }
    }

    private static final class SetBackedInitializationState
            implements SessionEagerLoader.SessionInitializationState {
        private final Set<TerminalSession> initialized;

        SetBackedInitializationState(Set<TerminalSession> initialized) {
            this.initialized = initialized;
        }

        @Override
        public boolean isSessionInitialized(@NonNull TerminalSession session) {
            return initialized.contains(session);
        }
    }

    private static final class RecordingInitializationAction
            implements SessionEagerLoader.SessionInitializationAction {
        final List<TerminalSession> initializedSessions = new ArrayList<>();
        private final Set<TerminalSession> initialized;

        RecordingInitializationAction(Set<TerminalSession> initialized) {
            this.initialized = initialized;
        }

        @Override
        public void initializeSession(@NonNull TerminalSession session) {
            initializedSessions.add(session);
            initialized.add(session);
        }
    }

    @Test
    public void initializesEveryNotYetInitializedSession() {
        TerminalSession first = newSession();
        TerminalSession second = newSession();
        TerminalSession third = newSession();
        Set<TerminalSession> initialized = new HashSet<>();
        RecordingInitializationAction action = new RecordingInitializationAction(initialized);

        SessionEagerLoader loader = new SessionEagerLoader(
            new RecordingSessionList(List.of(first, second, third)),
            new SetBackedInitializationState(initialized),
            action);

        loader.eagerLoadAllSessions();

        assertEquals(List.of(first, second, third), action.initializedSessions);
    }

    @Test
    public void skipsSessionsThatAreAlreadyInitialized() {
        TerminalSession alreadyInitialized = newSession();
        TerminalSession notInitialized = newSession();
        Set<TerminalSession> initialized = new HashSet<>();
        initialized.add(alreadyInitialized);
        RecordingInitializationAction action = new RecordingInitializationAction(initialized);

        SessionEagerLoader loader = new SessionEagerLoader(
            new RecordingSessionList(List.of(alreadyInitialized, notInitialized)),
            new SetBackedInitializationState(initialized),
            action);

        loader.eagerLoadAllSessions();

        assertEquals(List.of(notInitialized), action.initializedSessions);
    }

    @Test
    public void initializesEachSessionExactlyOnceAcrossRepeatedStartupCalls() {
        TerminalSession first = newSession();
        TerminalSession second = newSession();
        Set<TerminalSession> initialized = new HashSet<>();
        RecordingInitializationAction action = new RecordingInitializationAction(initialized);

        SessionEagerLoader loader = new SessionEagerLoader(
            new RecordingSessionList(List.of(first, second)),
            new SetBackedInitializationState(initialized),
            action);

        loader.eagerLoadAllSessions();
        loader.eagerLoadAllSessions();
        loader.eagerLoadAllSessions();

        assertEquals(List.of(first, second), action.initializedSessions);
    }

    @Test
    public void skipsNullSessionsInTheList() {
        TerminalSession session = newSession();
        List<TerminalSession> sessions = new ArrayList<>();
        sessions.add(null);
        sessions.add(session);
        Set<TerminalSession> initialized = new HashSet<>();
        RecordingInitializationAction action = new RecordingInitializationAction(initialized);

        SessionEagerLoader loader = new SessionEagerLoader(
            new RecordingSessionList(sessions),
            new SetBackedInitializationState(initialized),
            action);

        loader.eagerLoadAllSessions();

        assertEquals(List.of(session), action.initializedSessions);
        assertTrue(initialized.contains(session));
    }

    @Test
    public void doesNothingWhenThereAreNoSessions() {
        Set<TerminalSession> initialized = new HashSet<>();
        RecordingInitializationAction action = new RecordingInitializationAction(initialized);

        SessionEagerLoader loader = new SessionEagerLoader(
            new RecordingSessionList(new ArrayList<>()),
            new SetBackedInitializationState(initialized),
            action);

        loader.eagerLoadAllSessions();

        assertTrue(action.initializedSessions.isEmpty());
    }
}
