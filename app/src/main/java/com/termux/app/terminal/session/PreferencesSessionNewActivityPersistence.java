package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.app.terminal.SessionNewActivityPersistence;
import com.termux.app.terminal.SessionNewActivityState;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class PreferencesSessionNewActivityPersistence implements SessionNewActivityPersistence {

    private static final String LOG_TAG = "SessionNewActivityPersistence";

    @NonNull
    private final SessionNewActivityStateStore mStore;

    @NonNull
    private final SessionNewActivityStateSerializer mSerializer = new SessionNewActivityStateSerializer();

    public PreferencesSessionNewActivityPersistence(@NonNull SessionNewActivityStateStore store) {
        mStore = store;
    }

    @NonNull
    @Override
    public List<SessionNewActivityState> load() {
        try {
            return mSerializer.deserialize(mStore.getPersistedSessionNewActivityStates());
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to deserialize persisted session bell state", error);
            mStore.setPersistedSessionNewActivityStates(null);
            return new ArrayList<>();
        }
    }

    @Override
    public void save(@NonNull List<SessionNewActivityState> states) {
        try {
            mStore.setPersistedSessionNewActivityStates(mSerializer.serialize(states));
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize persisted session bell state", error);
        }
    }
}
