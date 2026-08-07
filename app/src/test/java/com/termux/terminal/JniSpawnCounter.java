package com.termux.terminal;

public final class JniSpawnCounter {

    private JniSpawnCounter() {
    }

    public static void reset() {
        JNI.resetCounters();
    }

    public static int createSubprocessCallCount() {
        return JNI.createSubprocessCallCount;
    }

    public static int eagerInitCallCount() {
        return JNI.eagerInitCallCount;
    }

    public static int eagerLoadAllSessionsCallCount() {
        return JNI.eagerLoadAllSessionsCallCount;
    }

    public static java.util.List<String> callSites() {
        return JNI.callSites;
    }

    public static boolean stubIsActive() {
        int callCountBeforeProbe = JNI.createSubprocessCallCount;
        int callSiteCountBeforeProbe = JNI.callSites.size();
        JNI.createSubprocess("probe", null, new String[0], new String[0], new int[1], 1, 1, 1, 1);
        boolean active = JNI.createSubprocessCallCount == callCountBeforeProbe + 1;
        JNI.createSubprocessCallCount = callCountBeforeProbe;
        JNI.callSites.subList(callSiteCountBeforeProbe, JNI.callSites.size()).clear();
        return active;
    }
}
