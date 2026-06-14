package com.termux.app.terminal;

import android.view.KeyEvent;

import com.termux.app.terminal.VolumeKeysSessionSwitchDecision.Action;

import org.junit.Assert;
import org.junit.Test;

public class VolumeKeysSessionSwitchDecisionTest {

    @Test
    public void switchesToNextSessionOnVolumeDownPressWhenEnabled() {
        Assert.assertEquals(Action.SWITCH_TO_NEXT_SESSION,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_VOLUME_DOWN, true));
    }

    @Test
    public void switchesToPreviousSessionOnVolumeUpPressWhenEnabled() {
        Assert.assertEquals(Action.SWITCH_TO_PREVIOUS_SESSION,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_VOLUME_UP, true));
    }

    @Test
    public void consumesVolumeKeyReleaseWithoutSwitchingSoSystemVolumeDoesNotChange() {
        Assert.assertEquals(Action.CONSUME_WITHOUT_SWITCH,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_VOLUME_DOWN, false));
        Assert.assertEquals(Action.CONSUME_WITHOUT_SWITCH,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_VOLUME_UP, false));
    }

    @Test
    public void ignoresVolumeKeysWhenPreferenceDisabledSoSystemVolumeChanges() {
        Assert.assertEquals(Action.IGNORE,
            VolumeKeysSessionSwitchDecision.decide(false, KeyEvent.KEYCODE_VOLUME_DOWN, true));
        Assert.assertEquals(Action.IGNORE,
            VolumeKeysSessionSwitchDecision.decide(false, KeyEvent.KEYCODE_VOLUME_UP, false));
    }

    @Test
    public void ignoresNonVolumeKeysEvenWhenEnabled() {
        Assert.assertEquals(Action.IGNORE,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_ENTER, true));
        Assert.assertEquals(Action.IGNORE,
            VolumeKeysSessionSwitchDecision.decide(true, KeyEvent.KEYCODE_A, false));
    }
}
