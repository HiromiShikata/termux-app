package com.termux.app.ownercall;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public final class OwnerCallBodyTableCellBorder extends Drawable {

    private final int mStrokePixels;
    private final boolean mDrawsLeftEdge;
    private final boolean mDrawsTopEdge;
    @NonNull
    private final Paint mLinePaint;
    @NonNull
    private final Paint mFillPaint;

    public OwnerCallBodyTableCellBorder(int strokePixels, int lineColor, int fillColor,
                                        boolean drawsLeftEdge, boolean drawsTopEdge) {
        mStrokePixels = strokePixels;
        mDrawsLeftEdge = drawsLeftEdge;
        mDrawsTopEdge = drawsTopEdge;
        mLinePaint = new Paint();
        mLinePaint.setColor(lineColor);
        mFillPaint = new Paint();
        mFillPaint.setColor(fillColor);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (mFillPaint.getColor() != 0) {
            canvas.drawRect(bounds, mFillPaint);
        }
        canvas.drawRect(bounds.right - mStrokePixels, bounds.top, bounds.right, bounds.bottom,
            mLinePaint);
        canvas.drawRect(bounds.left, bounds.bottom - mStrokePixels, bounds.right, bounds.bottom,
            mLinePaint);
        if (mDrawsLeftEdge) {
            canvas.drawRect(bounds.left, bounds.top, bounds.left + mStrokePixels, bounds.bottom,
                mLinePaint);
        }
        if (mDrawsTopEdge) {
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + mStrokePixels,
                mLinePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mLinePaint.setAlpha(alpha);
        mFillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mLinePaint.setColorFilter(colorFilter);
        mFillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
