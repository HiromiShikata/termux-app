package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class HostTmuxSessionKillCommand {

    private static final String SESSION_NAME_PLACEHOLDER = "{name}";

    private HostTmuxSessionKillCommand() {
    }

    public static boolean hasCommandTemplate(String commandTemplate) {
        return commandTemplate != null && !commandTemplate.trim().isEmpty();
    }

    public static String forSessionName(String sessionName, String commandTemplate) {
        if (sessionName == null || sessionName.isEmpty() || !hasCommandTemplate(commandTemplate)) {
            return null;
        }
        return commandTemplate.trim().replace(SESSION_NAME_PLACEHOLDER,
            SessionDefinitionPlanner.shellQuote(HostTmuxSessionName.normalize(sessionName)));
    }
}
