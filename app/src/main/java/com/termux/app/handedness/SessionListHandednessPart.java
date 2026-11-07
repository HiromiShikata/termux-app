package com.termux.app.handedness;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.terminal.ShortcutFlowLayout;

public final class SessionListHandednessPart implements HandednessLayoutPart {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final LinearLayoutGravityMirror gravityMirror = new LinearLayoutGravityMirror();
    private final TextViewGravityMirror textGravityMirror = new TextViewGravityMirror();
    private final TextView title;
    private final ViewGroup titleRow;
    private final LinearLayout controlsGroup;
    private final ShortcutFlowLayout shortcuts;

    public SessionListHandednessPart(View activityRoot) {
        title = activityRoot.findViewById(R.id.session_list_bottom_sheet_title);
        titleRow = (ViewGroup) title.getParent();
        controlsGroup = activityRoot.findViewById(R.id.session_list_bottom_sheet_controls_group);
        shortcuts = activityRoot.findViewById(R.id.session_list_bottom_sheet_always_session_shortcuts_container);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        childOrderMirror.apply(titleRow, handedness);
        textGravityMirror.apply(title, handedness);
        gravityMirror.apply(controlsGroup, handedness);
        childOrderMirror.apply(controlsGroup, handedness);
        shortcuts.setStartAligned(handedness == Handedness.LEFT_HANDED);
    }

}
