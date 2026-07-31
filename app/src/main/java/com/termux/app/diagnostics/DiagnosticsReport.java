package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public final class DiagnosticsReport {

    @NonNull
    private final String mVersionName;
    private final int mVersionCode;
    private final long mReportTimestampMillis;

    private final int mSessionsCountedTowardCap;
    private final int mSessionsDisplayedCount;
    private final int mMaxSessionsCap;

    @NonNull
    private final List<DiagnosticsSessionLine> mSessionLines;

    private final int mOpenTabCount;
    private final int mTabHistoryEntryCount;

    private final boolean mWakeLockHeld;
    private final boolean mForeground;

    @NonNull
    private final List<DiagnosticEvent> mRecentEvents;

    @NonNull
    private final DiagnosticsMemoryUsage mMemoryUsage;
    @NonNull
    private final DiagnosticsWorkCostLine mBackgroundOutputScanCost;
    @NonNull
    private final DiagnosticsWorkCostLine mBufferReflowCost;
    @NonNull
    private final DiagnosticsMainThreadStalls mMainThreadStalls;
    @NonNull
    private final DiagnosticsMainLooperQueue mMainLooperQueue;
    private final long mProcessUptimeMillis;

    public DiagnosticsReport(@NonNull String versionName, int versionCode, long reportTimestampMillis,
                             int sessionsCountedTowardCap, int sessionsDisplayedCount, int maxSessionsCap,
                             @NonNull List<DiagnosticsSessionLine> sessionLines,
                             int openTabCount, int tabHistoryEntryCount,
                             boolean wakeLockHeld, boolean foreground,
                             @NonNull List<DiagnosticEvent> recentEvents,
                             @NonNull DiagnosticsMemoryUsage memoryUsage,
                             @NonNull DiagnosticsWorkCostLine backgroundOutputScanCost,
                             @NonNull DiagnosticsWorkCostLine bufferReflowCost,
                             @NonNull DiagnosticsMainThreadStalls mainThreadStalls,
                             @NonNull DiagnosticsMainLooperQueue mainLooperQueue,
                             long processUptimeMillis) {
        mVersionName = versionName;
        mVersionCode = versionCode;
        mReportTimestampMillis = reportTimestampMillis;
        mSessionsCountedTowardCap = sessionsCountedTowardCap;
        mSessionsDisplayedCount = sessionsDisplayedCount;
        mMaxSessionsCap = maxSessionsCap;
        mSessionLines = Collections.unmodifiableList(sessionLines);
        mOpenTabCount = openTabCount;
        mTabHistoryEntryCount = tabHistoryEntryCount;
        mWakeLockHeld = wakeLockHeld;
        mForeground = foreground;
        mRecentEvents = Collections.unmodifiableList(recentEvents);
        mMemoryUsage = memoryUsage;
        mBackgroundOutputScanCost = backgroundOutputScanCost;
        mBufferReflowCost = bufferReflowCost;
        mMainThreadStalls = mainThreadStalls;
        mMainLooperQueue = mainLooperQueue;
        mProcessUptimeMillis = processUptimeMillis;
    }

    @NonNull
    public String getVersionName() {
        return mVersionName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public long getReportTimestampMillis() {
        return mReportTimestampMillis;
    }

    public int getSessionsCountedTowardCap() {
        return mSessionsCountedTowardCap;
    }

    public int getSessionsDisplayedCount() {
        return mSessionsDisplayedCount;
    }

    public int getOrphanedSessionCount() {
        int orphaned = mSessionsCountedTowardCap - mSessionsDisplayedCount;
        return orphaned > 0 ? orphaned : 0;
    }

    public int getMaxSessionsCap() {
        return mMaxSessionsCap;
    }

    @NonNull
    public List<DiagnosticsSessionLine> getSessionLines() {
        return mSessionLines;
    }

    public int getOpenTabCount() {
        return mOpenTabCount;
    }

    public int getTabHistoryEntryCount() {
        return mTabHistoryEntryCount;
    }

    public boolean isWakeLockHeld() {
        return mWakeLockHeld;
    }

    public boolean isForeground() {
        return mForeground;
    }

    @NonNull
    public List<DiagnosticEvent> getRecentEvents() {
        return mRecentEvents;
    }

    @NonNull
    public DiagnosticsMemoryUsage getMemoryUsage() {
        return mMemoryUsage;
    }

    @NonNull
    public DiagnosticsWorkCostLine getBackgroundOutputScanCost() {
        return mBackgroundOutputScanCost;
    }

    @NonNull
    public DiagnosticsWorkCostLine getBufferReflowCost() {
        return mBufferReflowCost;
    }

    @NonNull
    public DiagnosticsMainThreadStalls getMainThreadStalls() {
        return mMainThreadStalls;
    }

    @NonNull
    public DiagnosticsMainLooperQueue getMainLooperQueue() {
        return mMainLooperQueue;
    }

    public long getProcessUptimeMillis() {
        return mProcessUptimeMillis;
    }

    public long getTotalTranscriptRows() {
        long total = 0;
        for (DiagnosticsSessionLine line : mSessionLines) {
            total += line.getTranscriptRows();
        }
        return total;
    }
}
