package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class HostTmuxSessionKillCommand {

    private HostTmuxSessionKillCommand() {
    }

    public static String forSessionName(String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        return "tmux kill-session -t " + SessionDefinitionPlanner.shellQuote(sessionName) + "\n";
    }
}
