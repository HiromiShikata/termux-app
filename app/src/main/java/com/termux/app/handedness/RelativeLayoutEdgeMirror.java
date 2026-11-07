package com.termux.app.handedness;

import android.view.View;
import android.widget.RelativeLayout;

import com.termux.R;

final class RelativeLayoutEdgeMirror {

    void apply(View child, Handedness handedness) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) child.getLayoutParams();
        EdgeState original = (EdgeState) child.getTag(R.id.handedness_original_edge_tag);
        if (original == null) {
            original = EdgeState.capture(params);
            child.setTag(R.id.handedness_original_edge_tag, original);
        }
        if (handedness == Handedness.LEFT_HANDED) {
            params.addRule(RelativeLayout.ALIGN_PARENT_START, original.alignParentRightRule);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, original.alignParentLeftRule);
            params.setMarginStart(original.marginEnd);
            params.setMarginEnd(original.marginStart);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START, original.alignParentLeftRule);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, original.alignParentRightRule);
            params.setMarginStart(original.marginStart);
            params.setMarginEnd(original.marginEnd);
        }
        child.setLayoutParams(params);
    }

    private static final class EdgeState {

        private final int alignParentLeftRule;
        private final int alignParentRightRule;
        private final int marginStart;
        private final int marginEnd;

        private EdgeState(int alignParentLeftRule, int alignParentRightRule, int marginStart, int marginEnd) {
            this.alignParentLeftRule = alignParentLeftRule;
            this.alignParentRightRule = alignParentRightRule;
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
        }

        static EdgeState capture(RelativeLayout.LayoutParams params) {
            params.resolveLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            return new EdgeState(
                params.getRule(RelativeLayout.ALIGN_PARENT_LEFT),
                params.getRule(RelativeLayout.ALIGN_PARENT_RIGHT),
                params.getMarginStart(),
                params.getMarginEnd());
        }

    }

}
