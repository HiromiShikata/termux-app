package com.termux.app.sessiondefinition;

import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionPlanner {

    public List<SessionDefinitionPlannedSession> plan(List<SessionDefinitionEntry> entries, String commandTemplate) {
        List<SessionDefinitionPlannedSession> plannedSessions = new ArrayList<>();
        String template = commandTemplate == null ? "" : commandTemplate.trim();

        for (SessionDefinitionEntry entry : entries) {
            for (String url : entry.getUrls()) {
                String command = buildCommand(template, url);
                plannedSessions.add(new SessionDefinitionPlannedSession(url, command));
            }
        }
        return plannedSessions;
    }

    private String buildCommand(String template, String name) {
        if (template.isEmpty()) {
            return null;
        }
        return template.replace("{name}", shellQuote(name));
    }

    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
