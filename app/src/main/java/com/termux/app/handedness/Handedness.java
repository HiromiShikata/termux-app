package com.termux.app.handedness;

public enum Handedness {
    RIGHT_HANDED,
    LEFT_HANDED;

    public static Handedness fromLeftHandedModeEnabled(boolean leftHandedModeEnabled) {
        return leftHandedModeEnabled ? LEFT_HANDED : RIGHT_HANDED;
    }
}
