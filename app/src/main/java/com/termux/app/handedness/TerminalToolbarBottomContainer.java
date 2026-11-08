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
        View switchButton = findViewById(R.id.terminal_toolbar_handedness_switch_button);
        int containerWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int switchButtonWidth = switchButton.getLayoutParams().width;
        boolean pagerShown = pager.getVisibility() != View.GONE;
        if (pagerShown) {
            setFixedWidth(pager, containerWidth / 2 - switchButtonWidth);
        }
        setFixedWidth(column, pagerShown ? containerWidth / 2 : containerWidth - switchButtonWidth);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private static void setFixedWidth(View child, int width) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
        params.width = width;
        params.weight = 0;
    }

}
