package com.termux.app.ownercall;

public final class OwnerCallDialogWindowLocationStability {

    private OwnerCallDialogWindowLocationStability() {
    }

    public static boolean hasSettled(int previousWindowRelativeYPixels,
                                      int previousWindowHeightPixels,
                                      int currentWindowRelativeYPixels,
                                      int currentWindowHeightPixels,
                                      int viewHeightPixels) {
        boolean withinWindowBounds = currentWindowRelativeYPixels >= 0
            && currentWindowRelativeYPixels + viewHeightPixels <= currentWindowHeightPixels;
        boolean unchangedSincePreviousCheck = currentWindowRelativeYPixels == previousWindowRelativeYPixels
            && currentWindowHeightPixels == previousWindowHeightPixels;
        return withinWindowBounds && unchangedSincePreviousCheck;
    }
}
