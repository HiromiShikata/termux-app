package com.termux.terminal;

final class JNI {

    static final int INVALID_FILE_DESCRIPTOR = -1;

    private static final int CALL_SITE_DESCRIPTION_LENGTH_LIMIT = 400;

    private static final int STUB_PROCESS_ID_BASE = 10000;

    private static final String SWEEP_EAGER_INIT_METHOD_NAME =
        "eagerInitializeReconnectedBackgroundSessionEmulator";

    private static final String EAGER_LOAD_ALL_SESSIONS_METHOD_NAME = "eagerLoadSessionEmulator";

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
                if (SWEEP_EAGER_INIT_METHOD_NAME.equals(element.getMethodName())) {
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
        processId[0] = STUB_PROCESS_ID_BASE + createSubprocessCallCount;
        return INVALID_FILE_DESCRIPTOR;
    }

    private static String callSiteLabel(boolean fromEagerInit, boolean fromEagerLoadAllSessions) {
        if (fromEagerInit) return "[EAGER] ";
        if (fromEagerLoadAllSessions) return "[EAGER_LOAD_ALL_SESSIONS] ";
        return "[OTHER] ";
    }

    public static void setPtyWindowSize(int fd, int rows, int cols, int cellWidth, int cellHeight) {
    }

    public static int waitFor(int processId) {
        return 0;
    }

    public static void close(int fileDescriptor) {
    }
}
