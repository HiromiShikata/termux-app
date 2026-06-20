package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public interface SessionNewActivityPersistence {

    @NonNull
    List<SessionNewActivityState> load();

    void save(@NonNull List<SessionNewActivityState> states);
}
