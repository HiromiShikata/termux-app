package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class HostTmuxCallToUserScrollCommand {

    public static final char DEFAULT_TMUX_PREFIX_KEY = 0x02;

    private static final String CALL_TO_USER_ANCHOR = "<call-to-user>";

    private HostTmuxCallToUserScrollCommand() {
    }

    public static String forSessionName(String sessionName) {
        return forSessionName(sessionName, DEFAULT_TMUX_PREFIX_KEY);
    }

    public static String forSessionName(String sessionName, char tmuxPrefixKey) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        String target = SessionDefinitionPlanner.shellQuote(sessionName);
        String anchor = SessionDefinitionPlanner.shellQuote(CALL_TO_USER_ANCHOR);
        return tmuxPrefixKey + ":copy-mode -t " + target + "\n"
            + tmuxPrefixKey + ":send-keys -t " + target + " -X search-backward " + anchor + "\n";
    }
}
