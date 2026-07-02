package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserTabHistoryPersistScheduler {

    public interface Debouncer {
        void schedule(@NonNull Runnable task);

        void cancel();
    }

    public interface BackgroundRunner {
        void run(@NonNull Runnable task);
    }

    public interface HistorySink {
        void write(@NonNull String serialized);
    }

    public interface HistorySerializer {
        @NonNull
        String serialize(@NonNull BrowserTabHistory history);
    }

    private final Debouncer mDebouncer;

    private final BackgroundRunner mBackgroundRunner;

    private final HistorySerializer mSerializer;

    private final HistorySink mSink;

    @Nullable
    private BrowserTabHistory mPendingHistory;

    public BrowserTabHistoryPersistScheduler(
        @NonNull Debouncer debouncer,
        @NonNull BackgroundRunner backgroundRunner,
        @NonNull HistorySerializer serializer,
        @NonNull HistorySink sink) {
        mDebouncer = debouncer;
        mBackgroundRunner = backgroundRunner;
        mSerializer = serializer;
        mSink = sink;
    }

    public void markDirty(@NonNull BrowserTabHistory history) {
        mPendingHistory = history;
        mDebouncer.schedule(this::persistPendingInBackground);
    }

    private void persistPendingInBackground() {
        BrowserTabHistory history = takePending();
        if (history == null) return;
        mBackgroundRunner.run(() -> mSink.write(mSerializer.serialize(history)));
    }

    public void flushNow() {
        mDebouncer.cancel();
        BrowserTabHistory history = takePending();
        if (history == null) return;
        mSink.write(mSerializer.serialize(history));
    }

    public boolean hasPendingWrite() {
        return mPendingHistory != null;
    }

    @Nullable
    private BrowserTabHistory takePending() {
        BrowserTabHistory history = mPendingHistory;
        mPendingHistory = null;
        return history;
    }
}
