package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

final class MarginEdgeMirror {

    void apply(View child, Handedness handedness) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        MarginState original = (MarginState) child.getTag(R.id.handedness_original_margin_tag);
        if (original == null) {
            original = new MarginState(params.getMarginStart(), params.getMarginEnd());
            child.setTag(R.id.handedness_original_margin_tag, original);
        }
        if (handedness == Handedness.LEFT_HANDED) {
            params.setMarginStart(original.end);
            params.setMarginEnd(original.start);
        } else {
            params.setMarginStart(original.start);
            params.setMarginEnd(original.end);
        }
        child.setLayoutParams(params);
    }

    private static final class MarginState {
        private final int start;
        private final int end;

        private MarginState(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

}
