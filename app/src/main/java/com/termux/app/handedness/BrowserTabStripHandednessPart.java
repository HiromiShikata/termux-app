package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

public final class BrowserTabStripHandednessPart implements HandednessLayoutPart {

    private final ViewGroup tabStripContainer;

    public BrowserTabStripHandednessPart(View activityRoot) {
        tabStripContainer = activityRoot.findViewById(R.id.browser_tab_strip_container);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        for (int index = 0; index < tabStripContainer.getChildCount(); index++) {
            View closeButton = tabStripContainer.getChildAt(index)
                .findViewById(R.id.browser_tab_strip_close_button);
            if (closeButton != null) {
                HandednessGravity.applyToFrameChild(closeButton, handedness);
            }
        }
    }

}
