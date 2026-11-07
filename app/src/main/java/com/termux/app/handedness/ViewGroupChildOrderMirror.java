package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ViewGroupChildOrderMirror {

    void apply(ViewGroup group, Handedness handedness) {
        apply(group, handedness, originalOrder -> {
            List<View> target = new ArrayList<>(originalOrder);
            Collections.reverse(target);
            return target;
        });
    }

    void apply(ViewGroup group, Handedness handedness, LeftHandedChildOrder leftHandedOrder) {
        @SuppressWarnings("unchecked")
        List<View> original = (List<View>) group.getTag(R.id.handedness_original_child_order_tag);
        if (original == null) {
            original = new ArrayList<>();
            for (int index = 0; index < group.getChildCount(); index++) {
                original.add(group.getChildAt(index));
            }
            group.setTag(R.id.handedness_original_child_order_tag, original);
        }
        List<View> target = handedness == Handedness.LEFT_HANDED
            ? leftHandedOrder.arrange(original) : new ArrayList<>(original);
        for (View child : target) {
            group.bringChildToFront(child);
        }
    }

}
