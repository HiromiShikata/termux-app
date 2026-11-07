package com.termux.app.handedness;

import android.view.View;
import android.widget.LinearLayout;

import com.termux.R;

public final class TerminalToolbarHandednessPart implements HandednessLayoutPart {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final LinearLayoutGravityMirror gravityMirror = new LinearLayoutGravityMirror();
    private final LinearLayout bottomContainer;
    private final LinearLayout iconButtonRow;
    private final LinearLayout textInputRow;

    public TerminalToolbarHandednessPart(View activityRoot) {
        bottomContainer = activityRoot.findViewById(R.id.terminal_toolbar_bottom_container);
        iconButtonRow = activityRoot.findViewById(R.id.terminal_toolbar_icon_button_row);
        textInputRow = activityRoot.findViewById(R.id.terminal_toolbar_text_input_row);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        childOrderMirror.apply(bottomContainer, handedness);
        gravityMirror.apply(iconButtonRow, handedness);
        childOrderMirror.apply(iconButtonRow, handedness);
        childOrderMirror.apply(textInputRow, handedness);
    }

}
