package com.termux.app.ownercall;

import android.view.View;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogRelayoutWatcherTest {

    @Test
    public void refitsTheDialogWhenTheTerminalAreaMovesAsAfterARotation() {
        View terminalArea = new View(RuntimeEnvironment.getApplication());
        AtomicInteger refits = new AtomicInteger();
        OwnerCallDialogRelayoutWatcher.watchTerminalArea(terminalArea, refits::incrementAndGet);

        terminalArea.layout(0, 0, 1080, 2280);
        terminalArea.layout(1035, 0, 2424, 1000);

        Assert.assertEquals(2, refits.get());
    }

    @Test
    public void leavesTheDialogAloneWhenALayoutPassReportsUnchangedBounds() {
        View terminalArea = new View(RuntimeEnvironment.getApplication());
        terminalArea.layout(0, 0, 1080, 2280);
        AtomicInteger refits = new AtomicInteger();
        OwnerCallDialogRelayoutWatcher.watchTerminalArea(terminalArea, refits::incrementAndGet);

        terminalArea.layout(0, 0, 1080, 2280);

        Assert.assertEquals(0, refits.get());
    }
}
