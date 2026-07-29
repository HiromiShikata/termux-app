package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.TransientCommandSessionName;

public final class KillHostSessionPlanner {

    private KillHostSessionPlanner() {
    }

    @NonNull
    public static KillHostSessionPlan plan(@Nullable String commandTemplate, @Nullable String sessionName) {
        if (!HostTmuxSessionKillCommand.hasCommandTemplate(commandTemplate)) {
            return KillHostSessionPlan.commandNotConfigured();
        }
        String transientSessionName = TransientCommandSessionName.forKillOfSession(sessionName);
        String command = HostTmuxSessionKillCommand.forSessionName(sessionName, commandTemplate);
        if (transientSessionName == null || command == null) {
            return KillHostSessionPlan.sessionNameMissing();
        }
        return KillHostSessionPlan.start(transientSessionName, command);
    }
}
