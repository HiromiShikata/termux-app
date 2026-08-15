package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogEdgeGrip {

    private static final int SMALLEST_UNGRIPPED_FRACTION_OF_A_SIDE = 3;

    private static final OwnerCallDialogEdgeGrip NOTHING_GRIPPED =
        new OwnerCallDialogEdgeGrip(false, false, false, false);

    private static final OwnerCallDialogEdgeGrip BOTTOM_RIGHT_CORNER =
        new OwnerCallDialogEdgeGrip(false, true, false, true);

    private final boolean mLeftEdgeGripped;
    private final boolean mRightEdgeGripped;
    private final boolean mTopEdgeGripped;
    private final boolean mBottomEdgeGripped;

    private OwnerCallDialogEdgeGrip(boolean leftEdgeGripped, boolean rightEdgeGripped,
                                    boolean topEdgeGripped, boolean bottomEdgeGripped) {
        mLeftEdgeGripped = leftEdgeGripped;
        mRightEdgeGripped = rightEdgeGripped;
        mTopEdgeGripped = topEdgeGripped;
        mBottomEdgeGripped = bottomEdgeGripped;
    }

    @NonNull
    public static OwnerCallDialogEdgeGrip nothingGripped() {
        return NOTHING_GRIPPED;
    }

    @NonNull
    public static OwnerCallDialogEdgeGrip bottomRightCorner() {
        return BOTTOM_RIGHT_CORNER;
    }

    @NonNull
    public static OwnerCallDialogEdgeGrip resolve(int touchXPixels, int touchYPixels,
                                                  int widthPixels, int heightPixels,
                                                  int edgeBandPixels, int headerBottomPixels) {
        if (widthPixels <= 0 || heightPixels <= 0) {
            return NOTHING_GRIPPED;
        }
        if (touchXPixels < 0 || touchYPixels < 0
            || touchXPixels > widthPixels || touchYPixels > heightPixels) {
            return NOTHING_GRIPPED;
        }
        int horizontalBandPixels = bandWithin(edgeBandPixels, widthPixels);
        int verticalBandPixels = bandWithin(edgeBandPixels, heightPixels);
        boolean leftEdgeGripped = touchXPixels <= horizontalBandPixels;
        boolean rightEdgeGripped = touchXPixels >= widthPixels - horizontalBandPixels;
        if (touchYPixels < headerBottomPixels && !leftEdgeGripped && !rightEdgeGripped) {
            return NOTHING_GRIPPED;
        }
        return new OwnerCallDialogEdgeGrip(leftEdgeGripped, rightEdgeGripped,
            touchYPixels <= verticalBandPixels,
            touchYPixels >= heightPixels - verticalBandPixels);
    }

    private static int bandWithin(int edgeBandPixels, int sidePixels) {
        return Math.max(0,
            Math.min(edgeBandPixels, sidePixels / SMALLEST_UNGRIPPED_FRACTION_OF_A_SIDE));
    }

    public boolean isLeftEdgeGripped() {
        return mLeftEdgeGripped;
    }

    public boolean isRightEdgeGripped() {
        return mRightEdgeGripped;
    }

    public boolean isTopEdgeGripped() {
        return mTopEdgeGripped;
    }

    public boolean isBottomEdgeGripped() {
        return mBottomEdgeGripped;
    }

    public boolean isAnyEdgeGripped() {
        return mLeftEdgeGripped || mRightEdgeGripped || mTopEdgeGripped || mBottomEdgeGripped;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OwnerCallDialogEdgeGrip)) {
            return false;
        }
        OwnerCallDialogEdgeGrip that = (OwnerCallDialogEdgeGrip) other;
        return mLeftEdgeGripped == that.mLeftEdgeGripped
            && mRightEdgeGripped == that.mRightEdgeGripped
            && mTopEdgeGripped == that.mTopEdgeGripped
            && mBottomEdgeGripped == that.mBottomEdgeGripped;
    }

    @Override
    public int hashCode() {
        return (((mLeftEdgeGripped ? 1 : 0) * 31 + (mRightEdgeGripped ? 1 : 0)) * 31
            + (mTopEdgeGripped ? 1 : 0)) * 31 + (mBottomEdgeGripped ? 1 : 0);
    }

    @NonNull
    @Override
    public String toString() {
        return "OwnerCallDialogEdgeGrip{left=" + mLeftEdgeGripped
            + ", right=" + mRightEdgeGripped
            + ", top=" + mTopEdgeGripped
            + ", bottom=" + mBottomEdgeGripped + "}";
    }
}
