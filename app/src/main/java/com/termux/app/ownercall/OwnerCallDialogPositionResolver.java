package com.termux.app.ownercall;

import androidx.annotation.Nullable;

public final class OwnerCallDialogPositionResolver {

    private OwnerCallDialogPositionResolver() {
    }

    public static int resolve(@Nullable Integer requestedBottomMargin, int defaultBottomMargin,
                               int minBottomMargin, int maxBottomMargin) {
        if (requestedBottomMargin == null) {
            return defaultBottomMargin;
        }
        return Math.max(minBottomMargin, Math.min(maxBottomMargin, requestedBottomMargin));
    }
}
