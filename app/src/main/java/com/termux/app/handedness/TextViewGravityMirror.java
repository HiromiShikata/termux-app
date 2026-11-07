package com.termux.app.handedness;

import android.widget.TextView;

import com.termux.R;

final class TextViewGravityMirror {

    void apply(TextView view, Handedness handedness) {
        Integer original = (Integer) view.getTag(R.id.handedness_original_text_gravity_tag);
        if (original == null) {
            original = view.getGravity();
            view.setTag(R.id.handedness_original_text_gravity_tag, original);
        }
        view.setGravity(HandednessGravity.mirrored(original, handedness));
    }

}
