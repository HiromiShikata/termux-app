package com.termux.app.diagnostics;

public enum DiagnosticEventType {
    SESSION_CREATED,
    SESSION_REMOVED,
    SESSION_EXITED,
    SESSION_RECONNECTED,
    RECONNECT_FAILED,
    MAX_SESSIONS_REACHED,
    WAKE_LOCK_ACQUIRED,
    WAKE_LOCK_RELEASED
}
