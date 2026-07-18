package com.termux.app.terminal.io;

import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.TextView;

import com.termux.app.terminal.MaxHeightScrollView;

final class HistoryEntryScrollBinder {

    static final int MAX_VISIBLE_LINES = 3;

    private HistoryEntryScrollBinder() {
    }

    static void bind(MaxHeightScrollView entryScroll, TextView entryView) {
        entryScroll.setMaxScrollHeightPixels(entryView.getLineHeight() * MAX_VISIBLE_LINES);
        entryScroll.scrollTo(0, 0);
        entryScroll.setOnTouchListener((view, event) -> {
            if (!view.canScrollVertically(1) && !view.canScrollVertically(-1)) {
                return false;
            }
            ViewParent parent = view.getParent();
            if (parent == null) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    parent.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    parent.requestDisallowInterceptTouchEvent(false);
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
