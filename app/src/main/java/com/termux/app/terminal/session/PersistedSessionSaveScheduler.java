package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class PersistedSessionSaveScheduler {

    public interface BackgroundRunner {
        void run(@NonNull Runnable task);
    }

    public interface Serializer {
        @Nullable String serialize(@NonNull List<PersistedSessionRestoreData> sessions);
    }

    public interface Sink {
        void write(@NonNull String serialized);
    }

    private final BackgroundRunner mBackgroundRunner;
    private final Serializer mSerializer;
    private final Sink mSink;

    public PersistedSessionSaveScheduler(
        @NonNull BackgroundRunner backgroundRunner,
        @NonNull Serializer serializer,
        @NonNull Sink sink) {
        mBackgroundRunner = backgroundRunner;
        mSerializer = serializer;
        mSink = sink;
    }

    public void save(@NonNull List<PersistedSessionRestoreData> snapshot) {
        mBackgroundRunner.run(() -> {
            String serialized = mSerializer.serialize(snapshot);
            if (serialized != null)
                mSink.write(serialized);
        });
    }
}
