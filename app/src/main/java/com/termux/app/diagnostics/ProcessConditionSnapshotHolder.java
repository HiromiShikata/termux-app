package com.termux.app.diagnostics;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProcessConditionSnapshotHolder {

    private static final String LOG_TAG = "ProcessConditionSnapshot";

    private static final ProcessConditionSnapshotHolder INSTANCE = new ProcessConditionSnapshotHolder();

    @NonNull
    private final ExecutorService mWriteExecutor =
        Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, LOG_TAG);
            thread.setDaemon(true);
            return thread;
        });

    @Nullable
    private ProcessConditionSnapshotStore mStore;

    @NonNull
    private ProcessConditionSnapshot mPreviousProcessCondition = ProcessConditionSnapshot.NOT_RECORDED;

    private ProcessConditionSnapshotHolder() {
    }

    @NonNull
    public static ProcessConditionSnapshotHolder getInstance() {
        return INSTANCE;
    }

    public synchronized void useStore(@NonNull ProcessConditionSnapshotStore store) {
        if (mStore != null) return;
        mStore = store;
        mPreviousProcessCondition = store.read();
    }

    @NonNull
    public synchronized ProcessConditionSnapshot getPreviousProcessCondition() {
        return mPreviousProcessCondition;
    }

    public void recordCurrentCondition(@NonNull ProcessConditionSnapshot snapshot) {
        ProcessConditionSnapshotStore store = storeInUse();
        if (store == null) return;
        mWriteExecutor.execute(() -> {
            try {
                store.write(snapshot);
            } catch (RuntimeException writeFailure) {
                Log.e(LOG_TAG, "the condition of this process was not written, so it will not"
                    + " outlive the process", writeFailure);
            }
        });
    }

    @Nullable
    private synchronized ProcessConditionSnapshotStore storeInUse() {
        return mStore;
    }
}
