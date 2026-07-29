package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionPlanner;
import com.termux.app.terminal.session.TransientCommandSessionName;

public final class ResetSessionPlanner {

    private ResetSessionPlanner() {
    }

    @NonNull
    public static ResetSessionPlan plan(@Nullable String commandTemplate, @Nullable String sessionName) {
        String template = commandTemplate == null ? "" : commandTemplate.trim();
        if (template.isEmpty()) {
            return ResetSessionPlan.commandNotConfigured();
        }
        String transientSessionName = TransientCommandSessionName.forResetOfSession(sessionName);
        if (transientSessionName == null) {
            return ResetSessionPlan.sessionNameMissing();
        }
        String quotedHostSessionName =
            SessionDefinitionPlanner.shellQuote(HostTmuxSessionName.normalize(sessionName));
        return ResetSessionPlan.start(transientSessionName, template.replace("{name}", quotedHostSessionName));
    }
}
