package com.termux.app.handedness;

import android.widget.LinearLayout;

import com.termux.R;

final class LinearLayoutGravityMirror {

    void apply(LinearLayout layout, Handedness handedness) {
        Integer original = (Integer) layout.getTag(R.id.handedness_original_gravity_tag);
        if (original == null) {
            original = layout.getGravity();
            layout.setTag(R.id.handedness_original_gravity_tag, original);
        }
        layout.setGravity(HandednessGravity.mirrored(original, handedness));
    }

}
