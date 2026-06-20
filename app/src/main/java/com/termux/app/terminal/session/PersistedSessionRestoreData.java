package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

public final class PersistedSessionRestoreData {

    @Nullable
    private final String mHandle;

    @Nullable
    private final String mName;

    @Nullable
    private final String mExecutablePath;

    @Nullable
    private final String[] mArguments;

    private final boolean mIsFailSafe;

    @Nullable
    private final String mWorkingDirectory;

    public PersistedSessionRestoreData(@Nullable String handle, @Nullable String name, @Nullable String executablePath,
                                       @Nullable String[] arguments, boolean isFailSafe, @Nullable String workingDirectory) {
        mHandle = handle;
        mName = name;
        mExecutablePath = executablePath;
        mArguments = arguments == null ? null : arguments.clone();
        mIsFailSafe = isFailSafe;
        mWorkingDirectory = workingDirectory;
    }

    @Nullable
    public String getHandle() {
        return mHandle;
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
        return mArguments == null ? null : mArguments.clone();
    }

    public boolean isFailSafe() {
        return mIsFailSafe;
    }

    @Nullable
    public String getWorkingDirectory() {
        return mWorkingDirectory;
    }
}
