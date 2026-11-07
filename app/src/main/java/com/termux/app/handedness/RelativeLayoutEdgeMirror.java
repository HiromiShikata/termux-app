package com.termux.app.handedness;

import android.view.View;
import android.widget.RelativeLayout;

import java.util.IdentityHashMap;
import java.util.Map;

final class RelativeLayoutEdgeMirror {

    private final Map<View, EdgeState> originalStates = new IdentityHashMap<>();

    void apply(View child, Handedness handedness) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) child.getLayoutParams();
        EdgeState original = originalStates.get(child);
        if (original == null) {
            original = EdgeState.capture(params);
            originalStates.put(child, original);
        }
        if (handedness == Handedness.LEFT_HANDED) {
            params.addRule(RelativeLayout.ALIGN_PARENT_START, original.alignParentEndRule);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, original.alignParentStartRule);
            params.setMarginStart(original.marginEnd);
            params.setMarginEnd(original.marginStart);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START, original.alignParentStartRule);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, original.alignParentEndRule);
            params.setMarginStart(original.marginStart);
            params.setMarginEnd(original.marginEnd);
        }
        child.setLayoutParams(params);
    }

    private static final class EdgeState {

        private final int alignParentStartRule;
        private final int alignParentEndRule;
        private final int marginStart;
        private final int marginEnd;

        private EdgeState(int alignParentStartRule, int alignParentEndRule, int marginStart, int marginEnd) {
            this.alignParentStartRule = alignParentStartRule;
            this.alignParentEndRule = alignParentEndRule;
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
        }

        static EdgeState capture(RelativeLayout.LayoutParams params) {
            return new EdgeState(
                params.getRule(RelativeLayout.ALIGN_PARENT_START),
                params.getRule(RelativeLayout.ALIGN_PARENT_END),
                params.getMarginStart(),
                params.getMarginEnd());
        }

    }

}
