package com.termux.app.handedness;

import android.view.Gravity;
import android.widget.LinearLayout;

import java.util.IdentityHashMap;
import java.util.Map;

final class LinearLayoutGravityMirror {

    private final Map<LinearLayout, Integer> originalGravities = new IdentityHashMap<>();

    void apply(LinearLayout layout, Handedness handedness) {
        Integer original = originalGravities.get(layout);
        if (original == null) {
            original = layout.getGravity();
            originalGravities.put(layout, original);
        }
        layout.setGravity(handedness == Handedness.LEFT_HANDED ? mirrored(original) : original);
    }

    private static int mirrored(int gravity) {
        int horizontal = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int mirroredHorizontal;
        if (horizontal == Gravity.END) {
            mirroredHorizontal = Gravity.START;
        } else if (horizontal == Gravity.START) {
            mirroredHorizontal = Gravity.END;
        } else {
            mirroredHorizontal = horizontal;
        }
        return (gravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | mirroredHorizontal;
    }

}
