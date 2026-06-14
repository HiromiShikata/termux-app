package com.termux.app.terminal;

/**
 * Pure helpers that compute the user-visible text and the per-session notification id for the
 * Android system notification posted when a background terminal session emits a terminal bell.
 *
 * <p>Kept free of any Android framework dependency so the logic can be unit tested directly.
 */
public final class SessionBellNotificationContent {

    /** Number of distinct per-session notification id slots derived from the session handle. */
    static final int NOTIFICATION_ID_SLOTS = 1000;

    private SessionBellNotificationContent() {}

    /**
     * Build the notification body text for a bell emitted by the session with the given name.
     *
     * @param sessionName The session name, which may be {@code null} or empty.
     * @return A non-null human readable description of the bell event.
     */
    public static String contentText(String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty())
            return "A background session rang the bell";
        return "Session \"" + sessionName.trim() + "\" rang the bell";
    }

    /**
     * Map a session handle to a stable, non-negative notification id offset from the given base so
     * that bells from different sessions show as separate notifications and a repeated bell from the
     * same session updates its existing notification rather than stacking.
     *
     * @param baseId The base notification id.
     * @param sessionHandle The session handle, which may be {@code null}.
     * @return A notification id in the range {@code [baseId, baseId + NOTIFICATION_ID_SLOTS)}.
     */
    public static int notificationId(int baseId, String sessionHandle) {
        if (sessionHandle == null) return baseId;
        return baseId + Math.floorMod(sessionHandle.hashCode(), NOTIFICATION_ID_SLOTS);
    }
}
