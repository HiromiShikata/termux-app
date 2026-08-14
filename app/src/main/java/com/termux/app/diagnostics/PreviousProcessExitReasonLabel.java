package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class PreviousProcessExitReasonLabel {

    private static final int REASON_UNKNOWN = 0;

    private static final int REASON_EXIT_SELF = 1;

    private static final int REASON_SIGNALED = 2;

    private static final int REASON_LOW_MEMORY = 3;

    private static final int REASON_CRASH = 4;

    private static final int REASON_CRASH_NATIVE = 5;

    private static final int REASON_ANR = 6;

    private static final int REASON_INITIALIZATION_FAILURE = 7;

    private static final int REASON_PERMISSION_CHANGE = 8;

    private static final int REASON_EXCESSIVE_RESOURCE_USAGE = 9;

    private static final int REASON_USER_REQUESTED = 10;

    private static final int REASON_USER_STOPPED = 11;

    private static final int REASON_DEPENDENCY_DIED = 12;

    private static final int REASON_OTHER = 13;

    private static final int REASON_FREEZER = 14;

    private static final int REASON_PACKAGE_STATE_CHANGE = 15;

    private static final int REASON_PACKAGE_UPDATED = 16;

    private PreviousProcessExitReasonLabel() {
    }

    @NonNull
    public static String of(int reason) {
        switch (reason) {
            case REASON_UNKNOWN:
                return "a reason the system did not record";
            case REASON_EXIT_SELF:
                return "the app ending itself";
            case REASON_SIGNALED:
                return "a signal sent to the process";
            case REASON_LOW_MEMORY:
                return "the system reclaiming memory";
            case REASON_CRASH:
                return "a crash in Java code";
            case REASON_CRASH_NATIVE:
                return "a crash in native code";
            case REASON_ANR:
                return "an unresponsive main thread";
            case REASON_INITIALIZATION_FAILURE:
                return "the app failing to start";
            case REASON_PERMISSION_CHANGE:
                return "a permission of the app changing";
            case REASON_EXCESSIVE_RESOURCE_USAGE:
                return "the system judging the app's resource use excessive";
            case REASON_USER_REQUESTED:
                return "the user asking for the app to end";
            case REASON_USER_STOPPED:
                return "the user stopping the app from settings";
            case REASON_DEPENDENCY_DIED:
                return "a process this app depends on ending";
            case REASON_OTHER:
                return "a reason the system grouped as other";
            case REASON_FREEZER:
                return "the system freezer";
            case REASON_PACKAGE_STATE_CHANGE:
                return "the installed package changing state";
            case REASON_PACKAGE_UPDATED:
                return "the app being updated";
            default:
                return "a reason this app has no name for, recorded as code " + reason;
        }
    }
}
