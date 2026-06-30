package com.termux.app.terminal;

import androidx.annotation.Nullable;

public final class HungSessionDetector {

    public static final long STALE_OUT_MAX_AGE_MILLIS = 10L * 60L * 1000L;

    public boolean isHung(@Nullable Long lastOutTimeMillis, long nowMillis) {
        if (lastOutTimeMillis == null) {
            return false;
        }
        return nowMillis - lastOutTimeMillis > STALE_OUT_MAX_AGE_MILLIS;
    }
}
