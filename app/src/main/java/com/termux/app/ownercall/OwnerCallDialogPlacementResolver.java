package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallDialogPlacementResolver {

    private OwnerCallDialogPlacementResolver() {
    }

    @NonNull
    public static OwnerCallDialogPlacement resolve(
        @Nullable OwnerCallDialogPlacement requestedPlacement,
        @NonNull OwnerCallDialogPlacement defaultPlacement,
        int availableWidthPixels,
        int availableHeightPixels,
        int minimumWidthPixels,
        int minimumHeightPixels) {
        OwnerCallDialogPlacement placement =
            requestedPlacement == null ? defaultPlacement : requestedPlacement;
        int width = clamp(placement.getWidthPixels(), minimumWidthPixels,
            Math.max(minimumWidthPixels, availableWidthPixels));
        int height = clamp(placement.getHeightPixels(), minimumHeightPixels,
            Math.max(minimumHeightPixels, availableHeightPixels));
        int leftMargin = clamp(placement.getLeftMarginPixels(), 0,
            Math.max(0, availableWidthPixels - width));
        int bottomMargin = clamp(placement.getBottomMarginPixels(), 0,
            Math.max(0, availableHeightPixels - height));
        return new OwnerCallDialogPlacement(leftMargin, bottomMargin, width, height);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
