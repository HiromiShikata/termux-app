package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallCalledAt {

    private OwnerCallCalledAt() {
    }

    @Nullable
    public static Long toEpochMillis(@Nullable String calledAt) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public static String describe(@Nullable String calledAt, long nowMillis) {
        throw new UnsupportedOperationException();
    }
}
