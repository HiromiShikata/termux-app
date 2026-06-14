package com.termux.app.terminal;

import android.view.KeyEvent;

public final class VolumeKeysSessionSwitchDecision {

    public enum Action {
        IGNORE,
        CONSUME_WITHOUT_SWITCH,
        SWITCH_TO_NEXT_SESSION,
        SWITCH_TO_PREVIOUS_SESSION
    }

    private VolumeKeysSessionSwitchDecision() {
    }

    public static Action decide(boolean volumeKeysSwitchSessionsEnabled, int keyCode, boolean actionDown) {
        if (!volumeKeysSwitchSessionsEnabled) {
            return Action.IGNORE;
        }
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return Action.IGNORE;
        }
        if (!actionDown) {
            return Action.CONSUME_WITHOUT_SWITCH;
        }
        return keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            ? Action.SWITCH_TO_NEXT_SESSION
            : Action.SWITCH_TO_PREVIOUS_SESSION;
    }
}
