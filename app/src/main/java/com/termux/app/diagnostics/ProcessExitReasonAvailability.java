package com.termux.app.diagnostics;

public final class ProcessExitReasonAvailability {

    private static final int FIRST_ANDROID_VERSION_KEEPING_THE_RECORD = 30;

    private ProcessExitReasonAvailability() {
    }

    public static boolean isRecordedBy(int androidVersion) {
        return androidVersion >= FIRST_ANDROID_VERSION_KEEPING_THE_RECORD;
    }
}
