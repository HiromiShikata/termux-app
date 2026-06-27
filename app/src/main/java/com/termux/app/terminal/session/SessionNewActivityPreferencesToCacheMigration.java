package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.app.terminal.SessionNewActivityState;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.util.List;

public final class SessionNewActivityPreferencesToCacheMigration {

    private static final String LOG_TAG = "SessionNewActivityPersistence";

    @NonNull
    private final SessionNewActivityStateStore mPreferencesStore;

    @NonNull
    private final FileSessionNewActivityPersistence mCachePersistence;

    @NonNull
    private final SessionNewActivityStateSerializer mSerializer = new SessionNewActivityStateSerializer();

    public SessionNewActivityPreferencesToCacheMigration(
        @NonNull SessionNewActivityStateStore preferencesStore,
        @NonNull FileSessionNewActivityPersistence cachePersistence) {
        mPreferencesStore = preferencesStore;
        mCachePersistence = cachePersistence;
    }

    public void migrate() {
        String legacyValue = mPreferencesStore.getPersistedSessionNewActivityStates();
        if (legacyValue == null || legacyValue.isEmpty()) {
            return;
        }
        try {
            List<SessionNewActivityState> states = mSerializer.deserialize(legacyValue);
            mCachePersistence.saveBlocking(SessionNewActivityStateCaps.capStates(states));
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Failed to migrate persisted session activity state to cache", error);
        }
        mPreferencesStore.setPersistedSessionNewActivityStates(null);
    }
}
