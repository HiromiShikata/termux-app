package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ResetSessionPlan {

    public enum Outcome {
        COMMAND_NOT_CONFIGURED,
        SESSION_NAME_MISSING,
        START
    }

    @NonNull
    private final Outcome outcome;

    @Nullable
    private final String sessionName;

    @Nullable
    private final String command;

    private ResetSessionPlan(@NonNull Outcome outcome, @Nullable String sessionName, @Nullable String command) {
        this.outcome = outcome;
        this.sessionName = sessionName;
        this.command = command;
    }

    @NonNull
    static ResetSessionPlan commandNotConfigured() {
        return new ResetSessionPlan(Outcome.COMMAND_NOT_CONFIGURED, null, null);
    }

    @NonNull
    static ResetSessionPlan sessionNameMissing() {
        return new ResetSessionPlan(Outcome.SESSION_NAME_MISSING, null, null);
    }

    @NonNull
    static ResetSessionPlan start(@NonNull String sessionName, @NonNull String command) {
        return new ResetSessionPlan(Outcome.START, sessionName, command);
    }

    @NonNull
    public Outcome getOutcome() {
        return outcome;
    }

    public boolean shouldStart() {
        return outcome == Outcome.START;
    }

    @Nullable
    public String getSessionName() {
        return sessionName;
    }

    @Nullable
    public String getCommand() {
        return command;
    }
}
