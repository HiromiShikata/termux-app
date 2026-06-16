package com.termux.app.terminal;

public final class SessionBellNotificationClearDecision {

    private SessionBellNotificationClearDecision() {
    }

    public static boolean shouldClear(boolean stillCurrentSession, boolean stillHasPendingNotification) {
        return stillCurrentSession && stillHasPendingNotification;
    }
}
