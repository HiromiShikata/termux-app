package com.termux.terminal;

import java.util.function.BooleanSupplier;

public final class TerminalInputDelivery {

    public static final String REMOTE_SHELL_CLIENT_DETACHED_REASON =
        "the remote shell client is not attached to this terminal";

    private TerminalInputDelivery() {
    }

    public static boolean reachesTheProgramReadingTheTerminal(
            boolean sessionRunsRemoteShellClient, BooleanSupplier terminalIsInCanonicalMode) {
        return !sessionRunsRemoteShellClient || !terminalIsInCanonicalMode.getAsBoolean();
    }
}
