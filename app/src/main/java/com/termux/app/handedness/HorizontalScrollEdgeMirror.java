package com.termux.app.handedness;

import android.view.View;
import android.widget.HorizontalScrollView;

import com.termux.R;

public final class HorizontalScrollEdgeMirror {

    public void apply(HorizontalScrollView scrollView, Handedness handedness) {
        EdgeState state = stateOf(scrollView);
        Handedness previous = state.handedness;
        state.handedness = handedness;
        if (handedness == Handedness.LEFT_HANDED && previous != Handedness.LEFT_HANDED) {
            state.scrollToEndOnNextLayout = true;
            scrollToEnd(scrollView);
        } else if (handedness == Handedness.RIGHT_HANDED && previous == Handedness.LEFT_HANDED) {
            state.scrollToEndOnNextLayout = false;
            scrollView.scrollTo(0, 0);
        }
    }

    private static EdgeState stateOf(HorizontalScrollView scrollView) {
        Object existing = scrollView.getTag(R.id.handedness_scroll_edge_tag);
        if (existing instanceof EdgeState) {
            return (EdgeState) existing;
        }
        EdgeState state = new EdgeState();
        scrollView.setTag(R.id.handedness_scroll_edge_tag, state);
        scrollView.addOnLayoutChangeListener(
            (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                onLayout((HorizontalScrollView) view, state));
        return state;
    }

    private static void onLayout(HorizontalScrollView scrollView, EdgeState state) {
        int viewportWidth = scrollView.getWidth();
        int contentWidth = scrollView.getChildAt(0).getWidth();
        boolean sizeChanged = viewportWidth != state.viewportWidth || contentWidth != state.contentWidth;
        state.viewportWidth = viewportWidth;
        state.contentWidth = contentWidth;
        if (state.handedness != Handedness.LEFT_HANDED) {
            return;
        }
        if (state.scrollToEndOnNextLayout || sizeChanged) {
            state.scrollToEndOnNextLayout = false;
            scrollToEnd(scrollView);
        }
    }

    private static void scrollToEnd(HorizontalScrollView scrollView) {
        View content = scrollView.getChildAt(0);
        int maxScrollX = Math.max(0, content.getWidth() - scrollView.getWidth());
        scrollView.scrollTo(maxScrollX, 0);
    }

    private static final class EdgeState {
        private Handedness handedness;
        private boolean scrollToEndOnNextLayout;
        private int viewportWidth = -1;
        private int contentWidth = -1;
    }

}
