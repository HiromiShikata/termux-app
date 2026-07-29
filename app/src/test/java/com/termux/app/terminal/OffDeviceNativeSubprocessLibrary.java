package com.termux.app.terminal;

import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

final class OffDeviceNativeSubprocessLibrary {

    private OffDeviceNativeSubprocessLibrary() {
    }

    static void tolerateItsAbsence(@NonNull Runnable action) {
        try {
            action.run();
        } catch (LinkageError absence) {
            assertItIsTheOnlyAbsence(absence);
        }
    }

    static void assertItIsTheOnlyAbsence(@NonNull LinkageError error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        String absorbedFailure = rootCause.getClass().getName() + ": " + rootCause.getMessage();
        assertTrue("a Java virtual machine run can only absorb the absence of the device-only native "
                + "subprocess library that TerminalSession.initializeEmulator loads after it has already "
                + "constructed the terminal emulator; every other failure is a real one and must surface "
                + "instead of being discarded, yet this run absorbed " + absorbedFailure,
            absorbedFailure.contains("UnsatisfiedLinkError")
                || absorbedFailure.contains("com.termux.terminal.JNI"));
    }
}
