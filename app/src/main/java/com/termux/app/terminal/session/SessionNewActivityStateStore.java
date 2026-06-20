package com.termux.app.terminal.session;

public interface SessionNewActivityStateStore {

    String getPersistedSessionNewActivityStates();

    void setPersistedSessionNewActivityStates(String value);
}
