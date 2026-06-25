package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionNewActivityIndicator {

    @NonNull
    private final SessionNewActivityTier tier;

    @NonNull
    private final String label;

    private SessionNewActivityIndicator(@NonNull SessionNewActivityTier tier, @NonNull String label) {
        this.tier = tier;
        this.label = label;
    }

    @NonNull
    public SessionNewActivityTier getTier() {
        return tier;
    }

    public boolean isVisible() {
        return tier != SessionNewActivityTier.NONE;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public static SessionNewActivityIndicator indicatorFor(@Nullable Long lastOutputActivityTimeMillis,
                                                           @Nullable Long lastExplicitCallTimeMillis,
                                                           @Nullable Long lastUserInputTimeMillis,
                                                           @Nullable Long lastSeenTimeMillis, long nowMillis) {
        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivityTimeMillis, lastExplicitCallTimeMillis, lastUserInputTimeMillis,
            lastSeenTimeMillis, nowMillis);
        switch (tier) {
            case RED:
                return new SessionNewActivityIndicator(SessionNewActivityTier.RED,
                    SessionNewActivityStore.formatRelativeTime(nowMillis - lastExplicitCallTimeMillis));
            case YELLOW:
                return new SessionNewActivityIndicator(SessionNewActivityTier.YELLOW,
                    SessionNewActivityStore.formatRelativeTime(nowMillis - lastOutputActivityTimeMillis));
            case GRAY:
                return new SessionNewActivityIndicator(SessionNewActivityTier.GRAY,
                    SessionNewActivityStore.formatRelativeTime(nowMillis - lastOutputActivityTimeMillis));
            case NONE:
            default:
                return new SessionNewActivityIndicator(SessionNewActivityTier.NONE, "");
        }
    }
}
