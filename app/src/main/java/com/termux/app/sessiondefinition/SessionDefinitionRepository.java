package com.termux.app.sessiondefinition;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SessionDefinitionRepository {

    public interface OnEntriesLoadedListener {
        void onEntriesLoaded();
    }

    public interface OnEntriesReadyListener {
        void onEntriesReady(@NonNull SessionDefinitionLoadResult result);
    }

    public interface OnLoadFailedListener {
        void onLoadFailed(@NonNull Exception exception);
    }

    private static final String LOG_TAG = "SessionDefinitionRepository";

    private static final SessionDefinitionLoadResult EMPTY_RESULT =
        new SessionDefinitionLoadResult(Collections.emptyList(), 0, Collections.emptyList());

    private final SessionDefinitionLoader loader;

    private final List<OnEntriesLoadedListener> listenersAwaitingTheLoadInFlight = new ArrayList<>();

    private SessionDefinitionLoadResult result = EMPTY_RESULT;

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
        return result.getEntries();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isLoading() {
        return loading;
    }

    public void load(@NonNull String baseUrl, @NonNull OnEntriesLoadedListener listener) {
        if (baseUrl.isEmpty()) {
            return;
        }
        if (loaded) {
            listener.onEntriesLoaded();
            return;
        }
        listenersAwaitingTheLoadInFlight.add(listener);
        if (loading) {
            return;
        }
        loading = true;
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                SessionDefinitionLoadResult loadResult = loader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    result = loadResult;
                    loaded = true;
                    loading = false;
                    notifyListenersAwaitingTheLoadInFlight();
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition entries from " + baseUrl, exception);
                mainThreadHandler.post(() -> {
                    loading = false;
                    listenersAwaitingTheLoadInFlight.clear();
                });
            }
        }).start();
    }

    private void notifyListenersAwaitingTheLoadInFlight() {
        List<OnEntriesLoadedListener> listenersToNotify = new ArrayList<>(listenersAwaitingTheLoadInFlight);
        listenersAwaitingTheLoadInFlight.clear();
        for (OnEntriesLoadedListener listener : listenersToNotify) {
            listener.onEntriesLoaded();
        }
    }

    public void loadForRebuild(@NonNull String baseUrl, @NonNull OnEntriesReadyListener onEntriesReady, @NonNull OnLoadFailedListener onLoadFailed) {
        loadForRebuild(baseUrl, false, onEntriesReady, onLoadFailed);
    }

    public void loadForRebuild(@NonNull String baseUrl, boolean forceRefresh, @NonNull OnEntriesReadyListener onEntriesReady, @NonNull OnLoadFailedListener onLoadFailed) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        if (loaded && !forceRefresh) {
            mainThreadHandler.post(() -> onEntriesReady.onEntriesReady(result));
            return;
        }
        new Thread(() -> {
            try {
                SessionDefinitionLoadResult loadResult = loader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    result = loadResult;
                    loaded = true;
                    onEntriesReady.onEntriesReady(loadResult);
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition from " + baseUrl, exception);
                mainThreadHandler.post(() -> onLoadFailed.onLoadFailed(exception));
            }
        }).start();
    }
}
