package com.termux.app.sessiondefinition;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.List;

public final class SessionDefinitionRepository {

    public interface OnEntriesLoadedListener {
        void onEntriesLoaded();
    }

    public interface OnEntriesReadyListener {
        void onEntriesReady(@NonNull List<SessionDefinitionEntry> entries);
    }

    public interface OnLoadFailedListener {
        void onLoadFailed(@NonNull Exception exception);
    }

    private static final String LOG_TAG = "SessionDefinitionRepository";

    private final SessionDefinitionLoader loader;

    private List<SessionDefinitionEntry> entries = Collections.emptyList();

    private boolean loaded;

    private boolean loading;

    public SessionDefinitionRepository() {
        this(new SessionDefinitionLoader(new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser()));
    }

    public SessionDefinitionRepository(@NonNull SessionDefinitionLoader loader) {
        this.loader = loader;
    }

    @NonNull
    public List<SessionDefinitionEntry> getCachedEntries() {
        return entries;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void load(@NonNull String baseUrl, @NonNull OnEntriesLoadedListener listener) {
        if (loaded || loading || baseUrl.isEmpty()) {
            return;
        }
        loading = true;
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> loadedEntries = loader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    entries = loadedEntries;
                    loaded = true;
                    loading = false;
                    listener.onEntriesLoaded();
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition entries from " + baseUrl, exception);
                mainThreadHandler.post(() -> loading = false);
            }
        }).start();
    }

    public void loadForRebuild(@NonNull String baseUrl, @NonNull OnEntriesReadyListener onEntriesReady, @NonNull OnLoadFailedListener onLoadFailed) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        if (loaded) {
            mainThreadHandler.post(() -> onEntriesReady.onEntriesReady(entries));
            return;
        }
        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> loadedEntries = loader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    entries = loadedEntries;
                    loaded = true;
                    onEntriesReady.onEntriesReady(loadedEntries);
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition from " + baseUrl, exception);
                mainThreadHandler.post(() -> onLoadFailed.onLoadFailed(exception));
            }
        }).start();
    }
}
