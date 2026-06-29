package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VolumeKeyPickerOverlaySuppressionTest {

    private static final boolean OVERLAY_ENABLED = true;
    private static final boolean OVERLAY_DISABLED = false;
    private static final boolean INSTANT = false;
    private static final boolean PREVIEW_FIRST = true;
    private static final boolean OVERLAY_HIDDEN = false;
    private static final boolean FORWARD = true;

    private static final class PresentationProbe {
        final AtomicBoolean switched = new AtomicBoolean(false);
        final AtomicInteger rendered = new AtomicInteger(0);
        final AtomicInteger shown = new AtomicInteger(0);
        final AtomicInteger hideScheduled = new AtomicInteger(0);
        final AtomicInteger commitScheduled = new AtomicInteger(0);

        void run(boolean overlayEnabled, VolumeKeyPickerMoveDecision decision) {
            VolumeKeyPickerPresentation.present(
                overlayEnabled,
                decision,
                () -> switched.set(true),
                rendered::incrementAndGet,
                shown::incrementAndGet,
                hideScheduled::incrementAndGet,
                commitScheduled::incrementAndGet);
        }
    }

    private static VolumeKeyPickerMoveDecision instantDecision() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        return VolumeKeyPickerMoveDecision.decide(INSTANT, OVERLAY_HIDDEN, -1, 1, visible, FORWARD);
    }

    private static VolumeKeyPickerMoveDecision previewFirstDecision() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        return VolumeKeyPickerMoveDecision.decide(PREVIEW_FIRST, OVERLAY_HIDDEN, -1, 1, visible, FORWARD);
    }

    @Test
    public void disabledOverlayStillSwitchesInstantlyWithoutShowingOverlay() {
        PresentationProbe probe = new PresentationProbe();
        probe.run(OVERLAY_DISABLED, instantDecision());
        Assert.assertTrue(probe.switched.get());
        Assert.assertEquals(0, probe.rendered.get());
        Assert.assertEquals(0, probe.shown.get());
        Assert.assertEquals(0, probe.hideScheduled.get());
        Assert.assertEquals(0, probe.commitScheduled.get());
    }

    @Test
    public void disabledOverlayStillSwitchesEvenInPreviewFirstModeWithoutShowingOverlay() {
        PresentationProbe probe = new PresentationProbe();
        probe.run(OVERLAY_DISABLED, previewFirstDecision());
        Assert.assertTrue(probe.switched.get());
        Assert.assertEquals(0, probe.rendered.get());
        Assert.assertEquals(0, probe.shown.get());
        Assert.assertEquals(0, probe.hideScheduled.get());
        Assert.assertEquals(0, probe.commitScheduled.get());
    }

    @Test
    public void enabledOverlayInstantModeShowsOverlayAndSwitches() {
        PresentationProbe probe = new PresentationProbe();
        probe.run(OVERLAY_ENABLED, instantDecision());
        Assert.assertTrue(probe.switched.get());
        Assert.assertEquals(1, probe.rendered.get());
        Assert.assertEquals(1, probe.shown.get());
        Assert.assertEquals(1, probe.hideScheduled.get());
        Assert.assertEquals(0, probe.commitScheduled.get());
    }

    @Test
    public void enabledOverlayPreviewFirstModeShowsOverlayWithoutSwitching() {
        PresentationProbe probe = new PresentationProbe();
        probe.run(OVERLAY_ENABLED, previewFirstDecision());
        Assert.assertFalse(probe.switched.get());
        Assert.assertEquals(1, probe.rendered.get());
        Assert.assertEquals(1, probe.shown.get());
        Assert.assertEquals(0, probe.hideScheduled.get());
        Assert.assertEquals(1, probe.commitScheduled.get());
    }

    @Test
    public void defaultPresentOverloadKeepsOverlayEnabledBehavior() {
        AtomicInteger shown = new AtomicInteger(0);
        AtomicBoolean switched = new AtomicBoolean(false);
        VolumeKeyPickerPresentation.present(
            instantDecision(),
            () -> switched.set(true),
            () -> { },
            shown::incrementAndGet,
            () -> { },
            () -> { });
        Assert.assertTrue(switched.get());
        Assert.assertEquals(1, shown.get());
    }
}
