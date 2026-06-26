package com.termux.app.terminal;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import com.termux.R;

public class MaxHeightScrollView extends ScrollView {

    private int mMaxHeightPixels;

    public MaxHeightScrollView(Context context) {
        super(context);
    }

    public MaxHeightScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        readMaxScrollHeight(context, attrs);
    }

    public MaxHeightScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readMaxScrollHeight(context, attrs);
    }

    private void readMaxScrollHeight(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView);
        mMaxHeightPixels = typedArray.getDimensionPixelSize(
            R.styleable.MaxHeightScrollView_maxScrollHeight, 0);
        typedArray.recycle();
    }

    public void setMaxScrollHeightPixels(int maxHeightPixels) {
        mMaxHeightPixels = maxHeightPixels;
        requestLayout();
    }

    public int getMaxScrollHeightPixels() {
        return mMaxHeightPixels;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMaxHeightPixels > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeightPixels, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
