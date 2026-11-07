package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ViewGroupChildOrderMirror {

    private final Map<ViewGroup, List<View>> originalChildOrders = new IdentityHashMap<>();

    void apply(ViewGroup group, Handedness handedness) {
        List<View> original = originalChildOrders.get(group);
        if (original == null) {
            original = new ArrayList<>();
            for (int index = 0; index < group.getChildCount(); index++) {
                original.add(group.getChildAt(index));
            }
            originalChildOrders.put(group, original);
        }
        List<View> target = new ArrayList<>(original);
        if (handedness == Handedness.LEFT_HANDED) {
            Collections.reverse(target);
        }
        for (View child : target) {
            group.bringChildToFront(child);
        }
    }

}
