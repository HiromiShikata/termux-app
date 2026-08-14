package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class PreviousProcessExitImportanceLabel {

    private static final int IMPORTANCE_FOREGROUND = 100;

    private static final int IMPORTANCE_FOREGROUND_SERVICE = 125;

    private static final int IMPORTANCE_TOP_SLEEPING = 325;

    private static final int IMPORTANCE_VISIBLE = 200;

    private static final int IMPORTANCE_PERCEPTIBLE = 230;

    private static final int IMPORTANCE_SERVICE = 300;

    private static final int IMPORTANCE_CANT_SAVE_STATE = 350;

    private static final int IMPORTANCE_CACHED = 400;

    private static final int IMPORTANCE_GONE = 1000;

    private PreviousProcessExitImportanceLabel() {
    }

    @NonNull
    public static String of(int importance) {
        switch (importance) {
            case IMPORTANCE_FOREGROUND:
                return "in the foreground";
            case IMPORTANCE_FOREGROUND_SERVICE:
                return "running a foreground service";
            case IMPORTANCE_VISIBLE:
                return "visible to the user";
            case IMPORTANCE_PERCEPTIBLE:
                return "perceptible to the user";
            case IMPORTANCE_SERVICE:
                return "running a service";
            case IMPORTANCE_TOP_SLEEPING:
                return "on top with the screen off";
            case IMPORTANCE_CANT_SAVE_STATE:
                return "unable to save its state";
            case IMPORTANCE_CACHED:
                return "cached";
            case IMPORTANCE_GONE:
                return "already gone";
            default:
                return "an importance this app has no name for, recorded as " + importance;
        }
    }
}
