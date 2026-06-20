package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class InMemorySessionNewActivityPersistence implements SessionNewActivityPersistence {

    private List<SessionNewActivityState> mStates = new ArrayList<>();

    @NonNull
    @Override
    public List<SessionNewActivityState> load() {
        return new ArrayList<>(mStates);
    }

    @Override
    public void save(@NonNull List<SessionNewActivityState> states) {
        mStates = new ArrayList<>(states);
    }
}
