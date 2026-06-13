package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

public final class PersistedSession {

    @Nullable
    private final String mName;

    @Nullable
    private final String mExecutablePath;

    @Nullable
    private final String[] mArguments;

    private final boolean mIsFailSafe;

    @Nullable
    private final String mWorkingDirectory;

    public PersistedSession(@Nullable String name, @Nullable String executablePath, @Nullable String[] arguments,
                            boolean isFailSafe, @Nullable String workingDirectory) {
        mName = name;
        mExecutablePath = executablePath;
        mArguments = arguments;
        mIsFailSafe = isFailSafe;
        mWorkingDirectory = workingDirectory;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    @Nullable
    public String getExecutablePath() {
        return mExecutablePath;
    }

    @Nullable
    public String[] getArguments() {
        return mArguments;
    }

    public boolean isFailSafe() {
        return mIsFailSafe;
    }

    @Nullable
    public String getWorkingDirectory() {
        return mWorkingDirectory;
    }
}
