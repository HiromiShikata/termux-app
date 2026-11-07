package com.termux.app.handedness;

import android.view.View;
import android.widget.HorizontalScrollView;

import com.termux.R;

public final class HorizontalScrollEdgeMirror {

    public void apply(HorizontalScrollView scrollView, Handedness handedness) {
        if (scrollView.getTag(R.id.handedness_scroll_edge_tag) == null) {
            scrollView.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    scrollToEdge((HorizontalScrollView) view));
        }
        scrollView.setTag(R.id.handedness_scroll_edge_tag, handedness);
        scrollToEdge(scrollView);
    }

    static void scrollToEdge(HorizontalScrollView scrollView) {
        Handedness handedness = (Handedness) scrollView.getTag(R.id.handedness_scroll_edge_tag);
        View content = scrollView.getChildAt(0);
        int maxScrollX = Math.max(0, content.getWidth() - scrollView.getWidth());
        scrollView.scrollTo(handedness == Handedness.LEFT_HANDED ? maxScrollX : 0, 0);
    }

}
