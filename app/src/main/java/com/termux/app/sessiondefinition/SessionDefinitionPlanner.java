package com.termux.app.sessiondefinition;

import java.util.ArrayList;
import java.util.List;

public final class SessionDefinitionPlanner {

    private final SshKeepaliveCommandAugmenter sshKeepaliveCommandAugmenter =
        new SshKeepaliveCommandAugmenter();

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

    public SessionDefinitionPlannedSession planNamedSession(String name, String commandTemplate) {
        String template = commandTemplate == null ? "" : commandTemplate.trim();
        String command = name == null ? null : buildCommand(template, name);
        return new SessionDefinitionPlannedSession(name, command);
    }

    private String buildCommand(String template, String name) {
        if (template.isEmpty()) {
            return null;
        }
        String command = template.replace("{name}", shellQuote(name));
        return sshKeepaliveCommandAugmenter.augment(command);
    }

    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
