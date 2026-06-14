package com.termux.app.terminal.session;

public interface PersistedSessionStore {

    String getPersistedSessions();

    void setPersistedSessions(String value);
}
