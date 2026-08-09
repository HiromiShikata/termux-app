package com.termux.terminal;

/**
 * Counting stand-in for the native pseudoterminal bridge, on the unit-test classpath only.
 *
 * <p>It replaces the real class for every unit test in this module, not only for the cases that read
 * the counters, so its default behaviour MUST be the behaviour those cases already depend on: off a
 * device the real class fails its own static initializer in {@code System.loadLibrary("termux")}, so
 * every one of its methods raises a {@link LinkageError} at the call site. That is what {@code
 * OffDeviceNativeSubprocessLibrary} absorbs, and what makes {@code TerminalSession.initializeEmulator}
 * stop after it has constructed the terminal emulator and before it starts any reader thread.
 *
 * <p>A case that needs the spawn to return instead — because it measures how many spawns a code path
 * asks for rather than what happens after one — declares that for itself through {@link
 * JniSpawnCounter#pretendTheDeviceNativeSubprocessLibraryIsPresent()} and restores the default
 * afterwards.
 */
final class JNI {

    private static final int CALL_SITE_DESCRIPTION_LENGTH_LIMIT = 400;

    private static final int STUB_FILE_DESCRIPTOR_BASE = 900;

    private static final int STUB_PROCESS_ID_BASE = 10000;

    private static final String RECONNECT_PROCESS_START_METHOD_NAME =
        "startTheReconnectedBackgroundSessionProcess";

    private static final String EAGER_LOAD_ALL_SESSIONS_METHOD_NAME = "eagerLoadSessionEmulator";

    static boolean deviceNativeSubprocessLibraryIsPresent;

    static int createSubprocessCallCount;

    static int eagerInitCallCount;

    static int eagerLoadAllSessionsCallCount;

    static final java.util.List<String> callSites = new java.util.ArrayList<>();

    static void resetCounters() {
        createSubprocessCallCount = 0;
        eagerInitCallCount = 0;
        eagerLoadAllSessionsCallCount = 0;
        callSites.clear();
    }

    private static void raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary() {
        throw new UnsatisfiedLinkError("com.termux.terminal.JNI: the native subprocess library "
            + "termux is device-only and is absent from this Java virtual machine run");
    }

    public static int createSubprocess(String cmd, String cwd, String[] args, String[] envVars,
                                       int[] processId, int rows, int columns, int cellWidth,
                                       int cellHeight) {
        createSubprocessCallCount++;
        StringBuilder site = new StringBuilder();
        boolean fromEagerInit = false;
        boolean fromEagerLoadAllSessions = false;
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("com.termux.") && !className.equals("com.termux.terminal.JNI")) {
                if (RECONNECT_PROCESS_START_METHOD_NAME.equals(element.getMethodName())) {
                    fromEagerInit = true;
                }
                if (element.getMethodName().contains(EAGER_LOAD_ALL_SESSIONS_METHOD_NAME)) {
                    fromEagerLoadAllSessions = true;
                }
                if (site.length() < CALL_SITE_DESCRIPTION_LENGTH_LIMIT) {
                    site.append(className.substring(className.lastIndexOf('.') + 1))
                        .append('.').append(element.getMethodName()).append(" <- ");
                }
            }
        }
        if (fromEagerInit) eagerInitCallCount++;
        if (fromEagerLoadAllSessions) eagerLoadAllSessionsCallCount++;
        callSites.add(callSiteLabel(fromEagerInit, fromEagerLoadAllSessions) + site);
        if (!deviceNativeSubprocessLibraryIsPresent) {
            raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary();
        }
        processId[0] = STUB_PROCESS_ID_BASE + createSubprocessCallCount;
        return STUB_FILE_DESCRIPTOR_BASE + createSubprocessCallCount;
    }

    private static String callSiteLabel(boolean fromEagerInit, boolean fromEagerLoadAllSessions) {
        if (fromEagerInit) return "[EAGER] ";
        if (fromEagerLoadAllSessions) return "[EAGER_LOAD_ALL_SESSIONS] ";
        return "[OTHER] ";
    }

    public static void setPtyWindowSize(int fd, int rows, int cols, int cellWidth, int cellHeight) {
        if (!deviceNativeSubprocessLibraryIsPresent) {
            raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary();
        }
    }

    public static int waitFor(int processId) {
        if (!deviceNativeSubprocessLibraryIsPresent) {
            raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary();
        }
        return 0;
    }

    public static boolean isPtyInCanonicalMode(int fd) {
        if (!deviceNativeSubprocessLibraryIsPresent) {
            raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary();
        }
        return false;
    }

    public static void close(int fileDescriptor) {
        if (!deviceNativeSubprocessLibraryIsPresent) {
            raiseTheAbsenceOfTheDeviceNativeSubprocessLibrary();
        }
    }
}
