package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.SessionNewActivityPersistence;
import com.termux.app.terminal.SessionNewActivityState;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FileSessionNewActivityPersistence implements SessionNewActivityPersistence {

    private static final String LOG_TAG = "SessionNewActivityPersistence";

    @NonNull
    private final File mFile;

    @NonNull
    private final ExecutorService mWriteExecutor;

    @NonNull
    private final SessionNewActivityStateSerialization mSerialization;

    @Nullable
    private volatile List<SessionNewActivityState> mPendingStates;

    public FileSessionNewActivityPersistence(@NonNull File file) {
        this(file, Executors.newSingleThreadExecutor(), new SessionNewActivityStateSerializer());
    }

    public FileSessionNewActivityPersistence(@NonNull File file, @NonNull ExecutorService writeExecutor) {
        this(file, writeExecutor, new SessionNewActivityStateSerializer());
    }

    public FileSessionNewActivityPersistence(@NonNull File file, @NonNull ExecutorService writeExecutor,
                                             @NonNull SessionNewActivityStateSerialization serialization) {
        mFile = file;
        mWriteExecutor = writeExecutor;
        mSerialization = serialization;
    }

    @NonNull
    @Override
    public List<SessionNewActivityState> load() {
        try {
            return mSerialization.deserialize(readFile());
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to deserialize cached session activity state", error);
            return new ArrayList<>();
        }
    }

    @Override
    public void save(@NonNull List<SessionNewActivityState> states) {
        boolean writeAlreadyScheduled = mPendingStates != null;
        mPendingStates = new ArrayList<>(states);
        if (writeAlreadyScheduled) {
            return;
        }
        mWriteExecutor.execute(this::flushPending);
    }

    public void saveBlocking(@NonNull List<SessionNewActivityState> states) {
        try {
            writeFile(mSerialization.serialize(states));
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize session activity state", error);
        }
    }

    private void flushPending() {
        List<SessionNewActivityState> states = mPendingStates;
        mPendingStates = null;
        if (states == null) {
            return;
        }
        try {
            writeFile(mSerialization.serialize(states));
        } catch (JSONException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize session activity state", error);
        }
    }

    @Nullable
    private String readFile() {
        if (!mFile.exists()) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(mFile.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to read cached session activity state", error);
            return null;
        }
    }

    private void writeFile(@NonNull String serialized) {
        try {
            File parent = mFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.write(mFile.toPath(), serialized.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to write cached session activity state", error);
        }
    }
}
