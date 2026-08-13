package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogRelayoutWatcherTest {

    private static final int PORTRAIT_RIGHT = 1080;
    private static final int PORTRAIT_BOTTOM = 2400;
    private static final int LANDSCAPE_LEFT = 1200;
    private static final int LANDSCAPE_RIGHT = 2400;
    private static final int LANDSCAPE_BOTTOM = 1080;

    private static View terminalArea() {
        return new View(Robolectric.buildActivity(Activity.class).setup().get());
    }

    @Test
    public void redrawsTheDialogWhenTheTerminalAreaMovesToTheOtherHalfOfTheScreen() {
        View terminalArea = terminalArea();
        int[] redrawCount = new int[1];
        OwnerCallDialogRelayoutWatcher.watchTerminalArea(terminalArea, () -> redrawCount[0]++);

        terminalArea.layout(0, 0, PORTRAIT_RIGHT, PORTRAIT_BOTTOM);
        terminalArea.layout(LANDSCAPE_LEFT, 0, LANDSCAPE_RIGHT, LANDSCAPE_BOTTOM);

        assertEquals(2, redrawCount[0]);
    }

    @Test
    public void doesNotRedrawTheDialogWhenTheTerminalAreaKeepsTheSameBounds() {
        View terminalArea = terminalArea();
        int[] redrawCount = new int[1];
        OwnerCallDialogRelayoutWatcher.watchTerminalArea(terminalArea, () -> redrawCount[0]++);

        terminalArea.layout(0, 0, PORTRAIT_RIGHT, PORTRAIT_BOTTOM);
        terminalArea.layout(0, 0, PORTRAIT_RIGHT, PORTRAIT_BOTTOM);

        assertEquals(1, redrawCount[0]);
    }
}
