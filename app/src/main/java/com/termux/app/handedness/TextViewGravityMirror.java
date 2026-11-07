package com.termux.app.handedness;

import android.view.Gravity;
import android.widget.TextView;

import com.termux.R;

final class TextViewGravityMirror {

    void apply(TextView view, Handedness handedness) {
        Integer original = (Integer) view.getTag(R.id.handedness_original_text_gravity_tag);
        if (original == null) {
            original = view.getGravity();
            view.setTag(R.id.handedness_original_text_gravity_tag, original);
        }
        view.setGravity(handedness == Handedness.LEFT_HANDED ? mirrored(original) : original);
    }

    private static int mirrored(int gravity) {
        int horizontal = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int mirroredHorizontal = horizontal == Gravity.END ? Gravity.START
            : horizontal == Gravity.START ? Gravity.END : horizontal;
        return (gravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | mirroredHorizontal;
    }

}
