package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class HostTmuxCallToUserScrollCommand {

    private static final String CALL_TO_USER_ANCHOR = "<call-to-user>";

    private HostTmuxCallToUserScrollCommand() {
    }

    public static String forSessionName(String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        String target = SessionDefinitionPlanner.shellQuote(sessionName);
        String anchor = SessionDefinitionPlanner.shellQuote(CALL_TO_USER_ANCHOR);
        return "tmux copy-mode -t " + target + "\n"
            + "tmux send-keys -t " + target + " -X search-backward " + anchor + "\n";
    }
}
