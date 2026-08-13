package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public final class OwnerCallCalledAt {

    private static final String LOG_TAG = "OwnerCallCalledAt";

    private OwnerCallCalledAt() {
    }

    @Nullable
    public static Long toEpochMillis(@Nullable String calledAt) {
        if (calledAt == null || calledAt.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(calledAt.trim()).toInstant().toEpochMilli();
        } catch (DateTimeParseException unparsableCalledAt) {
            Logger.logWarn(LOG_TAG, "owner call time is not an ISO-8601 instant: " + calledAt);
            return null;
        }
    }

    @NonNull
    public static String describe(@Nullable String calledAt, long nowMillis) {
        Long epochMillis = toEpochMillis(calledAt);
        if (epochMillis == null) {
            return calledAt == null ? "" : calledAt;
        }
        return OwnerCallRelativeTime.of(epochMillis, nowMillis);
    }
}
