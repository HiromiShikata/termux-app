package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogPlacement {

    private final int mLeftMarginPixels;
    private final int mBottomMarginPixels;
    private final int mWidthPixels;
    private final int mHeightPixels;

    public OwnerCallDialogPlacement(int leftMarginPixels, int bottomMarginPixels, int widthPixels,
                                    int heightPixels) {
        mLeftMarginPixels = leftMarginPixels;
        mBottomMarginPixels = bottomMarginPixels;
        mWidthPixels = widthPixels;
        mHeightPixels = heightPixels;
    }

    public int getLeftMarginPixels() {
        return mLeftMarginPixels;
    }

    public int getBottomMarginPixels() {
        return mBottomMarginPixels;
    }

    public int getWidthPixels() {
        return mWidthPixels;
    }

    public int getHeightPixels() {
        return mHeightPixels;
    }

    @NonNull
    public OwnerCallDialogPlacement movedBy(int horizontalPixels, int verticalPixels) {
        return new OwnerCallDialogPlacement(mLeftMarginPixels + horizontalPixels,
            mBottomMarginPixels - verticalPixels, mWidthPixels, mHeightPixels);
    }

    @NonNull
    public OwnerCallDialogPlacement resizedBy(@NonNull OwnerCallDialogEdgeGrip grip,
                                              int horizontalPixels, int verticalPixels) {
        int leftMarginPixels = mLeftMarginPixels;
        int bottomMarginPixels = mBottomMarginPixels;
        int widthPixels = mWidthPixels;
        int heightPixels = mHeightPixels;
        if (grip.isLeftEdgeGripped()) {
            leftMarginPixels += horizontalPixels;
            widthPixels -= horizontalPixels;
        }
        if (grip.isRightEdgeGripped()) {
            widthPixels += horizontalPixels;
        }
        if (grip.isTopEdgeGripped()) {
            heightPixels -= verticalPixels;
        }
        if (grip.isBottomEdgeGripped()) {
            bottomMarginPixels -= verticalPixels;
            heightPixels += verticalPixels;
        }
        return new OwnerCallDialogPlacement(leftMarginPixels, bottomMarginPixels, widthPixels,
            heightPixels);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OwnerCallDialogPlacement)) {
            return false;
        }
        OwnerCallDialogPlacement that = (OwnerCallDialogPlacement) other;
        return mLeftMarginPixels == that.mLeftMarginPixels
            && mBottomMarginPixels == that.mBottomMarginPixels
            && mWidthPixels == that.mWidthPixels
            && mHeightPixels == that.mHeightPixels;
    }

    @Override
    public int hashCode() {
        return ((mLeftMarginPixels * 31 + mBottomMarginPixels) * 31 + mWidthPixels) * 31
            + mHeightPixels;
    }

    @NonNull
    @Override
    public String toString() {
        return "OwnerCallDialogPlacement{left=" + mLeftMarginPixels
            + ", bottom=" + mBottomMarginPixels
            + ", width=" + mWidthPixels
            + ", height=" + mHeightPixels + "}";
    }
}
