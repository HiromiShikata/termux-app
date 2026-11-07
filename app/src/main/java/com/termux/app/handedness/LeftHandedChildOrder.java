package com.termux.app.handedness;

import android.view.View;

import java.util.List;

interface LeftHandedChildOrder {

    List<View> arrange(List<View> rightHandedOrder);

}
