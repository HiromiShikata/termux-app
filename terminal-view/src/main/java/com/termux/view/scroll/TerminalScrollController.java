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
}
