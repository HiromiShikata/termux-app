package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public final class HandednessChildOrder {

    private HandednessChildOrder() {
    }

    public static void apply(ViewGroup group, Handedness handedness, List<View> leftHandedOrder) {
        new ViewGroupChildOrderMirror().apply(group, handedness, originalOrder -> leftHandedOrder);
    }

}
