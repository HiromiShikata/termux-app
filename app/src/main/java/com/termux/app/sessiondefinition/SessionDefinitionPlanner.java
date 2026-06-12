package com.termux.app.sessiondefinition;

import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionPlanner {

    public List<SessionDefinitionPlannedSession> plan(List<SessionDefinitionEntry> entries, String commandTemplate) {
        List<SessionDefinitionPlannedSession> plannedSessions = new ArrayList<>();
        String template = commandTemplate == null ? "" : commandTemplate.trim();

        for (SessionDefinitionEntry entry : entries) {
            List<String> urls = entry.getUrls();
            if (urls.isEmpty()) {
                plannedSessions.add(new SessionDefinitionPlannedSession(entry.getSessionName(), null));
                continue;
            }

            boolean multipleUrls = urls.size() > 1;
            for (int index = 0; index < urls.size(); index++) {
                String url = urls.get(index);
                String name = multipleUrls ? entry.getSessionName() + " " + (index + 1) : entry.getSessionName();
                String command = buildCommand(template, url, name);
                plannedSessions.add(new SessionDefinitionPlannedSession(name, command));
            }
        }
        return plannedSessions;
    }

    private String buildCommand(String template, String url, String name) {
        if (template.isEmpty()) {
            return null;
        }
        return template
            .replace("{url}", shellQuote(url))
            .replace("{name}", shellQuote(name));
    }

    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
