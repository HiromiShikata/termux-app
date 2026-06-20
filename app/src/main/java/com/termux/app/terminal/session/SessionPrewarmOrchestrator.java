package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionPrewarmOrchestrator {

    public interface SessionWarmAction {
        void warmSession(@NonNull String sessionHandle);
    }

    public interface BackgroundExecutor {
        void executeInBackground(@NonNull Runnable task);
    }

    public interface MainThreadPoster {
        void postToMainThread(@NonNull Runnable task);
    }

    private final SessionWarmAction warmAction;

    private final BackgroundExecutor backgroundExecutor;

    private final MainThreadPoster mainThreadPoster;

    private final Set<String> warmedSessionHandles = new LinkedHashSet<>();

    public SessionPrewarmOrchestrator(
        @NonNull SessionWarmAction warmAction,
        @NonNull BackgroundExecutor backgroundExecutor,
        @NonNull MainThreadPoster mainThreadPoster) {
        this.warmAction = warmAction;
        this.backgroundExecutor = backgroundExecutor;
        this.mainThreadPoster = mainThreadPoster;
    }

    public void prewarmSessions(@NonNull List<String> sessionHandles) {
        Set<String> sessionHandlesToWarm = new LinkedHashSet<>();
        for (String sessionHandle : sessionHandles) {
            if (sessionHandle == null) continue;
            if (warmedSessionHandles.contains(sessionHandle)) continue;
            sessionHandlesToWarm.add(sessionHandle);
        }
        if (sessionHandlesToWarm.isEmpty()) return;
        warmedSessionHandles.addAll(sessionHandlesToWarm);

        backgroundExecutor.executeInBackground(() -> {
            for (String sessionHandle : sessionHandlesToWarm) {
                mainThreadPoster.postToMainThread(() -> warmAction.warmSession(sessionHandle));
            }
        });
    }
}
