package com.termux.app.terminal;

import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionSwitchPickerOverlayTouchTest {

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void structurePanelHasNoMovementMethodSoItDoesNotConsumeTerminalTouches() {
        TextView structureView = new TextView(context());
        structureView.setMovementMethod(new ScrollingMovementMethod());

        SessionSwitchPickerController.configureStructureViewAsNonTouchTarget(structureView);

        Assert.assertNull(structureView.getMovementMethod());
    }

    @Test
    public void structurePanelIsNotClickableOrFocusableSoTouchesFallThroughToTheTerminal() {
        TextView structureView = new TextView(context());
        structureView.setClickable(true);
        structureView.setFocusable(true);
        structureView.setLongClickable(true);

        SessionSwitchPickerController.configureStructureViewAsNonTouchTarget(structureView);

        Assert.assertFalse(structureView.isClickable());
        Assert.assertFalse(structureView.isFocusable());
        Assert.assertFalse(structureView.isLongClickable());
    }
}
