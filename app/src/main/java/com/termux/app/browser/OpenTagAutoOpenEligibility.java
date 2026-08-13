package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class OpenTagAutoOpenEligibility {

    private OpenTagAutoOpenEligibility() {
    }

    public static boolean shouldAutoOpen(@Nullable Long lastSeenTimeMillis,
                                         @Nullable Long outputTimeMillis) {
        if (lastSeenTimeMillis == null) {
            return true;
        }
        if (outputTimeMillis == null) {
            return false;
        }
        return lastSeenTimeMillis < outputTimeMillis;
    }
}
