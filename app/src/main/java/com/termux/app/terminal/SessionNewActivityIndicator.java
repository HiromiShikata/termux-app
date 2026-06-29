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
    public static SessionNewActivityIndicator indicatorFor(@Nullable Long displayedOutTimeMillis,
                                                           @Nullable Long displayedReplyTimeMillis,
                                                           @Nullable Long pendingCallToUserTimeMillis,
                                                           @Nullable Long statuslineCallPendingTimeMillis,
                                                           @Nullable Long lastUserInputTimeMillis,
                                                           @Nullable Long lastSeenTimeMillis, long nowMillis) {
        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            displayedOutTimeMillis, displayedReplyTimeMillis, pendingCallToUserTimeMillis,
            statuslineCallPendingTimeMillis, lastUserInputTimeMillis, lastSeenTimeMillis, nowMillis);
        switch (tier) {
            case RED:
                return new SessionNewActivityIndicator(SessionNewActivityTier.RED,
                    SessionNewActivityStore.formatRelativeTime(nowMillis
                        - redCallTimeMillis(pendingCallToUserTimeMillis, statuslineCallPendingTimeMillis)));
            case YELLOW:
                return new SessionNewActivityIndicator(SessionNewActivityTier.YELLOW,
                    SessionNewActivityStore.formatRelativeTime(nowMillis
                        - mostRecent(displayedOutTimeMillis, displayedReplyTimeMillis)));
            case GRAY:
                return new SessionNewActivityIndicator(SessionNewActivityTier.GRAY,
                    SessionNewActivityStore.formatRelativeTime(nowMillis
                        - mostRecent(displayedOutTimeMillis, displayedReplyTimeMillis)));
            case NONE:
            default:
                return new SessionNewActivityIndicator(SessionNewActivityTier.NONE, "");
        }
    }

    private static long mostRecent(@Nullable Long firstTimeMillis, @Nullable Long secondTimeMillis) {
        if (firstTimeMillis == null) {
            return secondTimeMillis;
        }
        if (secondTimeMillis == null) {
            return firstTimeMillis;
        }
        return Math.max(firstTimeMillis, secondTimeMillis);
    }

    private static long redCallTimeMillis(@Nullable Long pendingCallToUserTimeMillis,
                                          @Nullable Long statuslineCallPendingTimeMillis) {
        if (pendingCallToUserTimeMillis != null) {
            return pendingCallToUserTimeMillis;
        }
        return statuslineCallPendingTimeMillis;
    }
}
