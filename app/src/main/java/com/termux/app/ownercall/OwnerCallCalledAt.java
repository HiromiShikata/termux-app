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
        } catch (DateTimeParseException unreadableCallTime) {
            Logger.logWarn(LOG_TAG, "an owner call carries the unreadable call time " + calledAt);
            return null;
        }
    }

    @NonNull
    public static String describe(@Nullable String calledAt, long nowMillis) {
        Long calledAtMillis = toEpochMillis(calledAt);
        if (calledAtMillis == null) {
            return calledAt == null ? "" : calledAt;
        }
        return OwnerCallElapsedTime.of(calledAtMillis, nowMillis);
    }
}
