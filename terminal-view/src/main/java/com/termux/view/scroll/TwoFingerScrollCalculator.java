package com.termux.view.scroll;

public final class TwoFingerScrollCalculator {

    private boolean tracking;
    private float lastFocalYPx;
    private float remainderPx;

    public boolean isTracking() {
        return tracking;
    }

    public void start(float focalYPx) {
        tracking = true;
        lastFocalYPx = focalYPx;
        remainderPx = 0f;
    }

    public void stop() {
        tracking = false;
        lastFocalYPx = 0f;
        remainderPx = 0f;
    }

    public int consumeRows(float focalYPx, float lineSpacingPx) {
        if (lineSpacingPx <= 0f) {
            return 0;
        }
        float deltaPx = (lastFocalYPx - focalYPx) + remainderPx;
        int rows = (int) (deltaPx / lineSpacingPx);
        remainderPx = deltaPx - rows * lineSpacingPx;
        lastFocalYPx = focalYPx;
        return rows;
    }
}
