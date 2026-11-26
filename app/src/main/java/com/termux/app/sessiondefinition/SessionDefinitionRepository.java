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

    private interface InFlightWaiter {
        void onLoaded(@NonNull SessionDefinitionLoadResult fetchedResult);
        void onFailed(@NonNull Exception exception);
    }

    private final List<InFlightWaiter> inFlightWaiters = new ArrayList<>();

    private final List<InFlightWaiter> queuedForcedWaiters = new ArrayList<>();

    private SessionDefinitionLoadResult result = EMPTY_RESULT;

    private boolean loaded;

    private boolean loading;

    private String inFlightBaseUrl;

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
        inFlightWaiters.add(new InFlightWaiter() {
            @Override
            public void onLoaded(@NonNull SessionDefinitionLoadResult fetchedResult) {
                listener.onEntriesLoaded();
            }

            @Override
            public void onFailed(@NonNull Exception exception) {
            }
        });
        if (loading) {
            return;
        }
        startFetch(baseUrl);
    }

    private void startFetch(@NonNull String baseUrl) {
        loading = true;
        inFlightBaseUrl = baseUrl;
        loadInFlight(baseUrl);
    }

    private void loadInFlight(@NonNull String baseUrl) {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                SessionDefinitionLoadResult loadResult = loader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    finishSuccessfulLoad(loadResult);
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition entries from " + baseUrl, exception);
                mainThreadHandler.post(() -> {
                    finishFailedLoad(exception);
                });
            }
        }).start();
    }

    private void finishSuccessfulLoad(@NonNull SessionDefinitionLoadResult fetchedResult) {
        if (fetchedResult.hasFailedGroups()) {
            List<SessionDefinitionEntry> mergedEntries = new ArrayList<>();
            for (SessionDefinitionEntry entry : fetchedResult.getEntries()) {
                if (!fetchedResult.getFailedGroupLabels().contains(entry.getGroupLabel())) {
                    mergedEntries.add(entry);
                }
            }
            for (SessionDefinitionEntry entry : result.getEntries()) {
                if (fetchedResult.getFailedGroupLabels().contains(entry.getGroupLabel())) {
                    mergedEntries.add(entry);
                }
            }
            result = new SessionDefinitionLoadResult(mergedEntries, fetchedResult.getTotalGroupCount(),
                fetchedResult.getFailedGroupLabels());
        } else {
            result = fetchedResult;
        }
        loaded = true;
        notifyInFlightWaiters(result, null);
        startQueuedFetchOrFinish();
    }

    private void finishFailedLoad(@NonNull Exception exception) {
        notifyInFlightWaiters(null, exception);
        startQueuedFetchOrFinish();
    }

    private void startQueuedFetchOrFinish() {
        if (queuedForcedWaiters.isEmpty()) {
            loading = false;
            return;
        }
        inFlightWaiters.addAll(queuedForcedWaiters);
        queuedForcedWaiters.clear();
        startFetch(inFlightBaseUrl);
    }

    private void notifyInFlightWaiters(SessionDefinitionLoadResult fetchedResult, Exception exception) {
        List<InFlightWaiter> waiters = new ArrayList<>(inFlightWaiters);
        inFlightWaiters.clear();
        for (InFlightWaiter waiter : waiters) {
            if (exception == null) {
                waiter.onLoaded(fetchedResult);
            } else {
                waiter.onFailed(exception);
            }
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
        InFlightWaiter waiter = new InFlightWaiter() {
            @Override
            public void onLoaded(@NonNull SessionDefinitionLoadResult fetchedResult) {
                onEntriesReady.onEntriesReady(fetchedResult);
            }

            @Override
            public void onFailed(@NonNull Exception exception) {
                onLoadFailed.onLoadFailed(exception);
            }
        };
        if (loading) {
            if (forceRefresh) {
                queuedForcedWaiters.add(waiter);
            } else {
                inFlightWaiters.add(waiter);
            }
            return;
        }
        inFlightWaiters.add(waiter);
        startFetch(baseUrl);
    }
}
