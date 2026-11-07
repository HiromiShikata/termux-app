package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

public final class MainScreenHandednessPart implements HandednessLayoutPart {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final RelativeLayoutEdgeMirror edgeMirror = new RelativeLayoutEdgeMirror();
    private final View appUpdateFloatingIndicator;
    private final ViewGroup sessionInfoBottomContainer;

    public MainScreenHandednessPart(View activityRoot) {
        appUpdateFloatingIndicator = activityRoot.findViewById(R.id.apk_update_floating_indicator);
        sessionInfoBottomContainer = activityRoot.findViewById(R.id.session_info_bottom_container);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        edgeMirror.apply(appUpdateFloatingIndicator, handedness);
        childOrderMirror.apply(sessionInfoBottomContainer, handedness);
    }

}
