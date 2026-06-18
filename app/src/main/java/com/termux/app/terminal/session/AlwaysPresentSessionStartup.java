package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AlwaysPresentSessionStartup {

    @NonNull
    private final String name;

    @Nullable
    private final String executablePath;

    @Nullable
    private final String[] arguments;

    public AlwaysPresentSessionStartup(@NonNull String name, @Nullable String executablePath,
                                       @Nullable String[] arguments) {
        this.name = name;
        this.executablePath = executablePath;
        this.arguments = arguments == null ? null : arguments.clone();
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getExecutablePath() {
        return executablePath;
    }

    @Nullable
    public String[] getArguments() {
        return arguments == null ? null : arguments.clone();
    }

    public boolean hasCommand() {
        return executablePath != null && arguments != null;
    }
}
