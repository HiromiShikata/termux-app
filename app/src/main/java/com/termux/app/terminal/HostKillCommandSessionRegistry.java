package com.termux.app.terminal;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HostKillCommandSessionRegistry {

    private final Map<String, String> hostSessionNameByCommandSessionHandle = new LinkedHashMap<>();

    public boolean isDispatchedFor(String hostSessionName) {
        if (hostSessionName == null) return false;
        return hostSessionNameByCommandSessionHandle.containsValue(hostSessionName);
    }

    public void record(String commandSessionHandle, String hostSessionName) {
        if (commandSessionHandle == null || hostSessionName == null) return;
        hostSessionNameByCommandSessionHandle.put(commandSessionHandle, hostSessionName);
    }

    public boolean forgetFinishedCommandSession(String commandSessionHandle) {
        if (commandSessionHandle == null) return false;
        return hostSessionNameByCommandSessionHandle.remove(commandSessionHandle) != null;
    }
}
