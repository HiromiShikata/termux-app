package com.termux.app.handedness;

import android.view.Gravity;
import android.widget.LinearLayout;

import com.termux.R;

final class LinearLayoutGravityMirror {

    void apply(LinearLayout layout, Handedness handedness) {
        Integer original = (Integer) layout.getTag(R.id.handedness_original_gravity_tag);
        if (original == null) {
            original = layout.getGravity();
            layout.setTag(R.id.handedness_original_gravity_tag, original);
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
