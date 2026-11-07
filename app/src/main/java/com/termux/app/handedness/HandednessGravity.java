package com.termux.app.handedness;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.termux.R;

public final class HandednessGravity {

    private HandednessGravity() {
    }

    public static int mirrored(int gravity, Handedness handedness) {
        if (handedness != Handedness.LEFT_HANDED) return gravity;
        int horizontal = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int mirroredHorizontal;
        if (horizontal == Gravity.START) {
            mirroredHorizontal = Gravity.END;
        } else if (horizontal == Gravity.END) {
            mirroredHorizontal = Gravity.START;
        } else {
            mirroredHorizontal = horizontal;
        }
        return (gravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | mirroredHorizontal;
    }

    public static void applyToFrameChild(View child, Handedness handedness) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) child.getLayoutParams();
        Integer original = (Integer) child.getTag(R.id.handedness_original_frame_gravity_tag);
        if (original == null) {
            original = params.gravity;
            child.setTag(R.id.handedness_original_frame_gravity_tag, original);
        }
        params.gravity = mirrored(original, handedness);
        child.setLayoutParams(params);
    }

}
