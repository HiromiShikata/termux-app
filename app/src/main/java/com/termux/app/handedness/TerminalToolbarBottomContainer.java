package com.termux.app.handedness;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.termux.R;

public final class TerminalToolbarBottomContainer extends LinearLayout {

    public TerminalToolbarBottomContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View pager = findViewById(R.id.terminal_toolbar_view_pager);
        View column = findViewById(R.id.terminal_toolbar_right_column);
        View leftColumn = findViewById(R.id.terminal_toolbar_left_column);
        int containerWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int leftColumnWidth = leftColumn.getLayoutParams().width;
        boolean pagerShown = pager.getVisibility() != View.GONE;
        if (pagerShown) {
            setFixedWidth(pager, containerWidth / 2 - leftColumnWidth);
        }
        setFixedWidth(column, pagerShown ? containerWidth / 2 : containerWidth - leftColumnWidth);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private static void setFixedWidth(View child, int width) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
        params.width = width;
        params.weight = 0;
    }

}
