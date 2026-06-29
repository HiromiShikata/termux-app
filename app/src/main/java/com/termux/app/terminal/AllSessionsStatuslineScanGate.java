package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class AllSessionsStatuslineScanGate {

    private final Map<String, Long> mLastScannedContentVersionBySession = new HashMap<>();

    public boolean shouldScan(@NonNull String sessionHandle, long contentVersion) {
        Long lastScannedContentVersion = mLastScannedContentVersionBySession.get(sessionHandle);
        if (lastScannedContentVersion != null && lastScannedContentVersion.longValue() == contentVersion) {
            return false;
        }
        mLastScannedContentVersionBySession.put(sessionHandle, contentVersion);
        return true;
    }

    public void markScanned(@NonNull String sessionHandle, long contentVersion) {
        mLastScannedContentVersionBySession.put(sessionHandle, contentVersion);
    }

    public void forget(@NonNull String sessionHandle) {
        mLastScannedContentVersionBySession.remove(sessionHandle);
    }
}
