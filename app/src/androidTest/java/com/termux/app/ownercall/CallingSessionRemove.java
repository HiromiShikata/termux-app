package com.termux.app.ownercall;

import androidx.test.core.app.ActivityScenario;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;

public final class CallingSessionRemove {

    private CallingSessionRemove() {
    }

    public static void removeEveryCallingSession(ActivityScenario<TermuxActivity> scenario) {
        if (scenario == null) {
            return;
        }
        scenario.onActivity(activity -> {
            TermuxService service = activity.getTermuxService();
            if (service == null) {
                return;
            }
            for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
                TerminalSession session = termuxSession.getTerminalSession();
                if (session == null
                    || !LocalOwnerCallServer.SESSION_URL.equals(session.mSessionName)) {
                    continue;
                }
                session.finishIfRunning();
                service.removeTermuxSession(session);
            }
        });
    }
}
