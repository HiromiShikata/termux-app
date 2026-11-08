package com.termux.app.handedness;

import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

public final class TerminalToolbarHandednessPart implements HandednessLayoutPart {

    private final ViewGroupChildOrderMirror childOrderMirror = new ViewGroupChildOrderMirror();
    private final LinearLayoutGravityMirror gravityMirror = new LinearLayoutGravityMirror();
    private final HorizontalScrollEdgeMirror scrollEdgeMirror = new HorizontalScrollEdgeMirror();
    private final LinearLayout bottomContainer;
    private final HorizontalScrollView iconButtonScroll;
    private final LinearLayout iconButtonRow;
    private final LinearLayout textInputRow;

    public TerminalToolbarHandednessPart(View activityRoot) {
        bottomContainer = activityRoot.findViewById(R.id.terminal_toolbar_bottom_container);
        iconButtonScroll = activityRoot.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        iconButtonRow = activityRoot.findViewById(R.id.terminal_toolbar_icon_button_row);
        textInputRow = activityRoot.findViewById(R.id.terminal_toolbar_text_input_row);
    }

    @Override
    public void applyHandedness(Handedness handedness) {
        childOrderMirror.apply(bottomContainer, handedness);
        gravityMirror.apply(iconButtonRow, handedness);
        childOrderMirror.apply(iconButtonRow, handedness);
        scrollEdgeMirror.apply(iconButtonScroll, handedness);
        childOrderMirror.apply(textInputRow, handedness);
    }

    public static void bindSwitchButton(View button, TermuxAppSharedPreferences preferences) {
        button.setOnClickListener(view -> preferences.setLeftHandedModeEnabled(!preferences.isLeftHandedModeEnabled()));
    }

}
