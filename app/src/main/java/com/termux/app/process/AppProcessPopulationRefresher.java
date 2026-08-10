package com.termux.app.process;

import androidx.annotation.NonNull;

import com.termux.app.diagnostics.AppProcessPopulationHolder;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AppProcessPopulationRefresher {

    public static final String READER_THREAD_NAME = "app-process-population-reader";

    @NonNull
    private final AtomicBoolean mReadInProgress = new AtomicBoolean(false);

    @NonNull
    private final AppProcessPopulationReader mReader;

    @NonNull
    private final AppProcessPopulationHolder mHolder;

    public AppProcessPopulationRefresher() {
        this(new AppProcessPopulationReader(), AppProcessPopulationHolder.getInstance());
    }

    public AppProcessPopulationRefresher(@NonNull AppProcessPopulationReader reader,
                                         @NonNull AppProcessPopulationHolder holder) {
        mReader = reader;
        mHolder = holder;
    }

    public boolean refreshOffTheMainThread() {
        if (!mReadInProgress.compareAndSet(false, true)) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    readAndRecord();
                } finally {
                    mReadInProgress.set(false);
                }
            }
        }, READER_THREAD_NAME).start();
        return true;
    }

    public void readAndRecord() {
        mHolder.record(mReader.read());
    }
}
