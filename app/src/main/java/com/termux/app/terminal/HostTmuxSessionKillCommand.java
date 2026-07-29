package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class HostTmuxSessionKillCommand {

    public static final char DEFAULT_TMUX_PREFIX_KEY = 0x02;

    private HostTmuxSessionKillCommand() {
    }

    public static String forSessionName(String sessionName) {
        return forSessionName(sessionName, DEFAULT_TMUX_PREFIX_KEY);
    }

    public static String forSessionName(String sessionName, char tmuxPrefixKey) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        String normalizedHostSessionName = HostTmuxSessionName.normalize(sessionName);
        return tmuxPrefixKey + ":kill-session -t " + SessionDefinitionPlanner.shellQuote(normalizedHostSessionName) + "\n";
    }
}
