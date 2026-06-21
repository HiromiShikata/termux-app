package com.termux.app.terminal;

import android.view.View;

public final class SessionSwitchPickerVisibility {

    private SessionSwitchPickerVisibility() {
    }

    public static int overlayVisibilityForShowing(boolean showing) {
        return showing ? View.VISIBLE : View.GONE;
    }
}
