package com.termux.view.scroll;

public final class TerminalScrollController {

    private final int mThumbWidthPx;
    private final int mTrackPaddingPx;
    private final int mMinThumbHeightPx;

    public TerminalScrollController(int thumbWidthPx, int trackPaddingPx, int minThumbHeightPx) {
        mThumbWidthPx = thumbWidthPx;
        mTrackPaddingPx = trackPaddingPx;
        mMinThumbHeightPx = minThumbHeightPx;
    }

    public boolean isScrollable(int transcriptRows) {
        return transcriptRows > 0;
    }

    public boolean shouldRenderControl(int transcriptRows, boolean alternateScreenActive) {
        return isScrollable(transcriptRows) || alternateScreenActive;
    }

    public int getThumbWidthPx() {
        return mThumbWidthPx;
    }

    public int getTrackTopPx() {
        return mTrackPaddingPx;
    }

    public int getTrackBottomPx(int viewHeightPx) {
        return viewHeightPx - mTrackPaddingPx;
    }

    public int getThumbLeftPx(int viewWidthPx) {
        return viewWidthPx - mThumbWidthPx - mTrackPaddingPx;
    }

    public int getThumbHeightPx(int viewHeightPx, int visibleRows, int totalRows) {
        int trackHeight = getTrackBottomPx(viewHeightPx) - getTrackTopPx();
        if (trackHeight <= 0 || totalRows <= 0 || visibleRows <= 0) return mMinThumbHeightPx;
        long scaled = (long) trackHeight * visibleRows / totalRows;
        int thumbHeight = (int) scaled;
        if (thumbHeight < mMinThumbHeightPx) thumbHeight = mMinThumbHeightPx;
        if (thumbHeight > trackHeight) thumbHeight = trackHeight;
        return thumbHeight;
    }

    public int getThumbTopPx(int viewHeightPx, int visibleRows, int totalRows, int topRow, int transcriptRows) {
        int trackTop = getTrackTopPx();
        int thumbHeight = getThumbHeightPx(viewHeightPx, visibleRows, totalRows);
        int travel = getTrackBottomPx(viewHeightPx) - trackTop - thumbHeight;
        if (travel <= 0 || transcriptRows <= 0) return trackTop;
        float fractionFromBottom = (float) (-topRow) / transcriptRows;
        if (fractionFromBottom < 0f) fractionFromBottom = 0f;
        if (fractionFromBottom > 1f) fractionFromBottom = 1f;
        float fractionFromTop = 1f - fractionFromBottom;
        return trackTop + Math.round(fractionFromTop * travel);
    }

    public int topRowForThumbCenter(int viewHeightPx, int visibleRows, int totalRows, float thumbCenterY, int transcriptRows) {
        int trackTop = getTrackTopPx();
        int thumbHeight = getThumbHeightPx(viewHeightPx, visibleRows, totalRows);
        int travel = getTrackBottomPx(viewHeightPx) - trackTop - thumbHeight;
        if (travel <= 0 || transcriptRows <= 0) return 0;
        float thumbTop = thumbCenterY - thumbHeight / 2f;
        float fractionFromTop = (thumbTop - trackTop) / travel;
        if (fractionFromTop < 0f) fractionFromTop = 0f;
        if (fractionFromTop > 1f) fractionFromTop = 1f;
        float fractionFromBottom = 1f - fractionFromTop;
        return -Math.round(fractionFromBottom * transcriptRows);
    }

    public boolean isWithinThumbGrabArea(int viewWidthPx, int viewHeightPx, int visibleRows, int totalRows, int topRow, int transcriptRows, float x, float y, int grabExtraPx) {
        if (!isScrollable(transcriptRows)) return false;
        int thumbLeft = getThumbLeftPx(viewWidthPx);
        if (x < thumbLeft - grabExtraPx) return false;
        int thumbTop = getThumbTopPx(viewHeightPx, visibleRows, totalRows, topRow, transcriptRows);
        int thumbBottom = thumbTop + getThumbHeightPx(viewHeightPx, visibleRows, totalRows);
        return y >= thumbTop - grabExtraPx && y <= thumbBottom + grabExtraPx;
    }

    public int getRelativeThumbHeightPx(int viewHeightPx) {
        int trackHeight = getTrackBottomPx(viewHeightPx) - getTrackTopPx();
        if (trackHeight <= 0) return mMinThumbHeightPx;
        if (mMinThumbHeightPx > trackHeight) return trackHeight;
        return mMinThumbHeightPx;
    }

    public int getRelativeThumbTopPx(int viewHeightPx) {
        int trackTop = getTrackTopPx();
        int trackBottom = getTrackBottomPx(viewHeightPx);
        int thumbHeight = getRelativeThumbHeightPx(viewHeightPx);
        int center = (trackTop + trackBottom) / 2;
        int thumbTop = center - thumbHeight / 2;
        if (thumbTop < trackTop) thumbTop = trackTop;
        if (thumbTop + thumbHeight > trackBottom) thumbTop = trackBottom - thumbHeight;
        return thumbTop;
    }

    public boolean isWithinRelativeThumbGrabArea(int viewWidthPx, int viewHeightPx, float x, float y, int grabExtraPx) {
        int thumbLeft = getThumbLeftPx(viewWidthPx);
        if (x < thumbLeft - grabExtraPx) return false;
        int thumbTop = getRelativeThumbTopPx(viewHeightPx);
        int thumbBottom = thumbTop + getRelativeThumbHeightPx(viewHeightPx);
        return y >= thumbTop - grabExtraPx && y <= thumbBottom + grabExtraPx;
    }

    public int wheelStepsForDragDelta(float dragDeltaPx, int rowHeightPx) {
        if (rowHeightPx <= 0) return 0;
        return (int) (dragDeltaPx / rowHeightPx);
    }
}
