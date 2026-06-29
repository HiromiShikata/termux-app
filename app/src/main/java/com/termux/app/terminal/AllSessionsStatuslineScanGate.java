package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class AllSessionsStatuslineScanGate {

    private final Map<String, Long> mLastScannedContentVersionBySession = new HashMap<>();

    /**
     * Whether the session whose screen content is at {@code contentVersion} should be parsed on this
     * tick. A session that already has stored statusline data is skipped when its screen content
     * version is unchanged since the last scan, avoiding a needless reparse of an idle screen. A
     * session that has no stored statusline data yet ({@code hasStoredStatuslineData} is false) is
     * always scanned regardless of the content version, because such a session keeps an unchanged
     * content version while never having parsed a statusline and would otherwise be skipped forever
     * and stay on the uncolored / {@code >1d} tier. The recorded version is still updated so that once
     * the session gains data, a later unchanged tick is skipped normally.
     */
    public boolean shouldScan(@NonNull String sessionHandle, long contentVersion,
                              boolean hasStoredStatuslineData) {
        Long lastScannedContentVersion = mLastScannedContentVersionBySession.get(sessionHandle);
        if (hasStoredStatuslineData
            && lastScannedContentVersion != null
            && lastScannedContentVersion.longValue() == contentVersion) {
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
