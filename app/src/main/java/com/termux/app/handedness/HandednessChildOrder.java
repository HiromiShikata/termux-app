package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

import java.util.ArrayList;
import java.util.List;

public final class HandednessChildOrder {

    private HandednessChildOrder() {
    }

    @SuppressWarnings("unchecked")
    public static void apply(ViewGroup group, Handedness handedness, List<View> leftHandedOrder) {
        List<View> original = (List<View>) group.getTag(R.id.handedness_original_child_order_tag);
        if (original == null) {
            original = new ArrayList<>();
            for (int index = 0; index < group.getChildCount(); index++) {
                original.add(group.getChildAt(index));
            }
            group.setTag(R.id.handedness_original_child_order_tag, original);
        }
        List<View> target = handedness == Handedness.LEFT_HANDED ? leftHandedOrder : original;
        for (View child : target) {
            group.bringChildToFront(child);
        }
    }

}
