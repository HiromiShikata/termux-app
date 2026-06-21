package com.termux.app.terminal;

import android.view.View;

import org.junit.Assert;
import org.junit.Test;

public class SessionSwitchPickerVisibilityTest {

    @Test
    public void overlayIsVisibleOnlyWhileShowing() {
        Assert.assertEquals(View.VISIBLE,
            SessionSwitchPickerVisibility.overlayVisibilityForShowing(true));
    }

    @Test
    public void overlayIsGoneWheneverNotShowingSoItCannotInterceptTerminalTouches() {
        Assert.assertEquals(View.GONE,
            SessionSwitchPickerVisibility.overlayVisibilityForShowing(false));
    }
}
