package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class ResetSessionCommand {

    public static final String SESSION_NAME_PREFIX = "reset ";

    private ResetSessionCommand() {
    }

    public static String forTemplateAndSessionName(String commandTemplate, String sessionName) {
        if (commandTemplate == null || sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        String template = commandTemplate.trim();
        if (template.isEmpty()) {
            return null;
        }
        String quotedHostSessionName =
            SessionDefinitionPlanner.shellQuote(HostTmuxSessionName.normalize(sessionName));
        return template.replace("{name}", quotedHostSessionName);
    }

    public static String sessionNameFor(String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        return SESSION_NAME_PREFIX + sessionName;
    }
}
