package com.termux.app.terminal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ShortcutFlowLayout extends ViewGroup {

    public ShortcutFlowLayout(@NonNull Context context) {
        super(context);
    }

    public ShortcutFlowLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ShortcutFlowLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        boolean widthBounded = widthMode != MeasureSpec.UNSPECIFIED;
        int availableWidth = widthBounded
            ? Math.max(0, widthSize - getPaddingLeft() - getPaddingRight())
            : Integer.MAX_VALUE;

        int rowWidth = 0;
        int rowHeight = 0;
        int widestRowWidth = 0;
        int stackedRowsHeight = 0;
        boolean rowHasChild = false;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams layoutParams = (MarginLayoutParams) child.getLayoutParams();
            int childOuterWidth = child.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
            int childOuterHeight = child.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
            if (rowHasChild && rowWidth + childOuterWidth > availableWidth) {
                widestRowWidth = Math.max(widestRowWidth, rowWidth);
                stackedRowsHeight += rowHeight;
                rowWidth = 0;
                rowHeight = 0;
                rowHasChild = false;
            }
            rowWidth += childOuterWidth;
            rowHeight = Math.max(rowHeight, childOuterHeight);
            rowHasChild = true;
        }
        widestRowWidth = Math.max(widestRowWidth, rowWidth);
        stackedRowsHeight += rowHeight;

        int contentWidth = widestRowWidth + getPaddingLeft() + getPaddingRight();
        int resolvedWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            resolvedWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            resolvedWidth = Math.min(contentWidth, widthSize);
        } else {
            resolvedWidth = contentWidth;
        }
        int contentHeight = stackedRowsHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolvedWidth, resolveSize(contentHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int availableWidth = Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight());
        int cursorX = getPaddingLeft();
        int cursorY = getPaddingTop();
        int rowHeight = 0;
        boolean rowHasChild = false;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }
            MarginLayoutParams layoutParams = (MarginLayoutParams) child.getLayoutParams();
            int childOuterWidth = child.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
            int childOuterHeight = child.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
            if (rowHasChild && cursorX - getPaddingLeft() + childOuterWidth > availableWidth) {
                cursorX = getPaddingLeft();
                cursorY += rowHeight;
                rowHeight = 0;
                rowHasChild = false;
            }
            int childLeft = cursorX + layoutParams.leftMargin;
            int childTop = cursorY + layoutParams.topMargin;
            child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(),
                childTop + child.getMeasuredHeight());
            cursorX += childOuterWidth;
            rowHeight = Math.max(rowHeight, childOuterHeight);
            rowHasChild = true;
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams source) {
        return new MarginLayoutParams(source);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams layoutParams) {
        return layoutParams instanceof MarginLayoutParams;
    }
}
