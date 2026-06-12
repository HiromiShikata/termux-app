package com.termux.app.sessiondefinition;

public final class SessionDefinitionPlannedSession {

    private final String name;
    private final String command;

    public SessionDefinitionPlannedSession(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
}
