package com.termux.terminal;

public final class TerminalInputDelivery {

    public static final String REMOTE_SHELL_CLIENT_DETACHED_REASON =
        "the remote shell client is not attached to this terminal";

    private TerminalInputDelivery() {
    }

    public static boolean reachesTheProgramReadingTheTerminal(
            boolean sessionRunsRemoteShellClient, boolean terminalIsInCanonicalMode) {
        return !sessionRunsRemoteShellClient || !terminalIsInCanonicalMode;
    }
}
