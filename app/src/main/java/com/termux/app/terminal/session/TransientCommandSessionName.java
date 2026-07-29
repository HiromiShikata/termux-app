package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

/**
 * Names the short-lived sessions that exist only to run a single command against the host and then
 * exit. The marker lives in the session name rather than in activity-scoped state because such a
 * session outlives the activity that started it, and every other per-session decision in this app is
 * already keyed by session name. A session carrying this name is never reconnected and is never
 * persisted, so a command session that has exited can never come back as a live ssh session aimed at
 * a host session that does not exist.
 */
public final class TransientCommandSessionName {

    public static final String RESET_PREFIX = "[reset] ";

    private TransientCommandSessionName() {
    }

    @Nullable
    public static String forResetOfSession(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        return RESET_PREFIX + sessionName;
    }

    public static boolean isTransient(@Nullable String sessionName) {
        return sessionName != null && sessionName.startsWith(RESET_PREFIX);
    }
}
