package com.termux.app.ownercall;

public final class OwnerCallDialogDragStep {

    private final int mHorizontalPixels;
    private final int mVerticalPixels;

    public OwnerCallDialogDragStep(int horizontalPixels, int verticalPixels) {
        mHorizontalPixels = horizontalPixels;
        mVerticalPixels = verticalPixels;
    }

    public int getHorizontalPixels() {
        return mHorizontalPixels;
    }

    public int getVerticalPixels() {
        return mVerticalPixels;
    }
}
