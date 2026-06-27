package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class BackgroundOutputScanGate {

    static final long MIN_SCAN_INTERVAL_MILLIS = 300L;

    private final Map<String, Long> mLastScannedContentVersionBySession = new HashMap<>();
    private final Map<String, Long> mLastScanTimeMillisBySession = new HashMap<>();

    public boolean shouldScan(@NonNull String sessionHandle, long contentVersion, long nowMillis) {
        Long lastScannedContentVersion = mLastScannedContentVersionBySession.get(sessionHandle);
        if (lastScannedContentVersion != null && lastScannedContentVersion.longValue() == contentVersion) {
            return false;
        }
        Long lastScanTimeMillis = mLastScanTimeMillisBySession.get(sessionHandle);
        if (lastScanTimeMillis != null && nowMillis - lastScanTimeMillis < MIN_SCAN_INTERVAL_MILLIS) {
            return false;
        }
        mLastScannedContentVersionBySession.put(sessionHandle, contentVersion);
        mLastScanTimeMillisBySession.put(sessionHandle, nowMillis);
        return true;
    }

    public void forget(@NonNull String sessionHandle) {
        mLastScannedContentVersionBySession.remove(sessionHandle);
        mLastScanTimeMillisBySession.remove(sessionHandle);
    }
}
