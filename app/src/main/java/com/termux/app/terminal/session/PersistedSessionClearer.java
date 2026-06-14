package com.termux.app.terminal.session;

public final class PersistedSessionClearer {

    private PersistedSessionClearer() {
    }

    public static boolean clear(PersistedSessionStore store) {
        String existing = store.getPersistedSessions();
        boolean hadPersistedSessions = existing != null && !existing.isEmpty();
        store.setPersistedSessions(null);
        return hadPersistedSessions;
    }
}
