package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;

public final class RestoredSessionCommandPlanner {

    public static final String SHELL_COMMAND_FLAG = "-c";

    private final SessionDefinitionPlanner mSessionDefinitionPlanner = new SessionDefinitionPlanner();

    @Nullable
    public String[] planArguments(@Nullable String sessionName,
                                  @Nullable String[] storedArguments,
                                  @Nullable String configuredCommandTemplate) {
        if (sessionName == null) {
            return storedArguments;
        }
        if (!isAShellCommandInvocation(storedArguments)) {
            return storedArguments;
        }
        String template = configuredCommandTemplate == null ? "" : configuredCommandTemplate.trim();
        if (template.isEmpty()) {
            return storedArguments;
        }
        String command = mSessionDefinitionPlanner.planNamedSession(sessionName, template).getCommand();
        if (command == null) {
            return storedArguments;
        }
        return new String[]{SHELL_COMMAND_FLAG, command};
    }

    private boolean isAShellCommandInvocation(@Nullable String[] storedArguments) {
        return storedArguments != null
            && storedArguments.length == 2
            && SHELL_COMMAND_FLAG.equals(storedArguments[0]);
    }
}
