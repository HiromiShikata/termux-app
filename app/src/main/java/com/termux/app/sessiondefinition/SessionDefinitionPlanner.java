package com.termux.app.sessiondefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionPlanner {

    private final DefaultProjectManagerSessionPlanner defaultProjectManagerSessionPlanner =
        new DefaultProjectManagerSessionPlanner();

    public List<SessionDefinitionPlannedSession> plan(List<SessionDefinitionEntry> entries, String commandTemplate) {
        List<SessionDefinitionPlannedSession> plannedSessions = new ArrayList<>();
        String template = commandTemplate == null ? "" : commandTemplate.trim();

        Set<String> projectLabelsWithPlacedManagerSession = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            String projectManagerSessionName =
                defaultProjectManagerSessionPlanner.sessionNameForProjectLabel(entry.getGroupLabel());
            if (projectManagerSessionName != null
                    && projectLabelsWithPlacedManagerSession.add(entry.getGroupLabel())) {
                plannedSessions.add(new SessionDefinitionPlannedSession(
                    projectManagerSessionName, buildCommand(template, projectManagerSessionName)));
            }
            for (String url : entry.getUrls()) {
                String command = buildCommand(template, url);
                plannedSessions.add(new SessionDefinitionPlannedSession(url, command));
            }
        }
        return plannedSessions;
    }

    public SessionDefinitionPlannedSession planNamedSession(String name, String commandTemplate) {
        String template = commandTemplate == null ? "" : commandTemplate.trim();
        String command = name == null ? null : buildCommand(template, name);
        return new SessionDefinitionPlannedSession(name, command);
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
