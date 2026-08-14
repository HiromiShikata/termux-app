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
    private final DiagnosticsWorkCostLine mForegroundOpenTagScanCost;
    @NonNull
    private final DiagnosticsWorkCostLine mBufferReflowCost;
    @NonNull
    private final DiagnosticsSessionReconnectCost mSessionReconnectCost;
    @NonNull
    private final DiagnosticsReplacedSessionShellInput mReplacedSessionShellInput;
    @NonNull
    private final DiagnosticsMainThreadStalls mMainThreadStalls;
    @NonNull
    private final DiagnosticsMainLooperQueue mMainLooperQueue;
    @NonNull
    private final ScrollbarViewCensus mScrollbarViewCensus;
    private final long mProcessUptimeMillis;
    @NonNull
    private final DiagnosticsBackgroundCycle mBackgroundCycle;
    @NonNull
    private final DiagnosticsVersionChange mVersionChange;
    @NonNull
    private final DiagnosticsShellExits mShellExits;
    @NonNull
    private final DiagnosticsPhantomProcessMonitor mPhantomProcessMonitor;
    @NonNull
    private final DiagnosticsAppProcessPopulation mAppProcessPopulation;
    @NonNull
    private final DiagnosticsWorkCostLine mTerminalDrawCost;
    @NonNull
    private final DiagnosticsWorkCostLine mShellOutputParseCost;
    @NonNull
    private final DiagnosticsSessionCreationPaths mSessionCreationPaths;
    @NonNull
    private final DiagnosticsActivityWindows mActivityWindows;
    @NonNull
    private final DiagnosticsReportDelivery mLastReportDelivery;
    @NonNull
    private final DiagnosticsMainLooperQueuePeak mMainLooperQueuePeak;
    @NonNull
    private final DiagnosticsScrollSteps mScrollSteps;
    @NonNull
    private final DiagnosticsTouchEvents mTouchEvents;
    @NonNull
    private final DiagnosticsPreviousProcessExits mPreviousProcessExits;

    public DiagnosticsReport(@NonNull String versionName, int versionCode, long reportTimestampMillis,
                             int sessionsCountedTowardCap, int sessionsDisplayedCount, int maxSessionsCap,
                             @NonNull List<DiagnosticsSessionLine> sessionLines,
                             int openTabCount, int tabHistoryEntryCount,
                             boolean wakeLockHeld, boolean foreground,
                             @NonNull List<DiagnosticEvent> recentEvents,
                             @NonNull DiagnosticsMemoryUsage memoryUsage,
                             @NonNull DiagnosticsWorkCostLine backgroundOutputScanCost,
                             @NonNull DiagnosticsWorkCostLine foregroundOpenTagScanCost,
                             @NonNull DiagnosticsWorkCostLine bufferReflowCost,
                             @NonNull DiagnosticsSessionReconnectCost sessionReconnectCost,
                             @NonNull DiagnosticsReplacedSessionShellInput replacedSessionShellInput,
                             @NonNull DiagnosticsMainThreadStalls mainThreadStalls,
                             @NonNull DiagnosticsMainLooperQueue mainLooperQueue,
                             @NonNull ScrollbarViewCensus scrollbarViewCensus,
                             long processUptimeMillis,
                             @NonNull DiagnosticsBackgroundCycle backgroundCycle,
                             @NonNull DiagnosticsVersionChange versionChange,
                             @NonNull DiagnosticsShellExits shellExits,
                             @NonNull DiagnosticsPhantomProcessMonitor phantomProcessMonitor,
                             @NonNull DiagnosticsAppProcessPopulation appProcessPopulation,
                             @NonNull DiagnosticsWorkCostLine terminalDrawCost,
                             @NonNull DiagnosticsWorkCostLine shellOutputParseCost,
                             @NonNull DiagnosticsSessionCreationPaths sessionCreationPaths,
                             @NonNull DiagnosticsActivityWindows activityWindows,
                             @NonNull DiagnosticsReportDelivery lastReportDelivery,
                             @NonNull DiagnosticsMainLooperQueuePeak mainLooperQueuePeak,
                             @NonNull DiagnosticsScrollSteps scrollSteps,
                             @NonNull DiagnosticsTouchEvents touchEvents,
                             @NonNull DiagnosticsPreviousProcessExits previousProcessExits) {
        mScrollSteps = scrollSteps;
        mTouchEvents = touchEvents;
        mPreviousProcessExits = previousProcessExits;
        mSessionCreationPaths = sessionCreationPaths;
        mActivityWindows = activityWindows;
        mLastReportDelivery = lastReportDelivery;
        mMainLooperQueuePeak = mainLooperQueuePeak;
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
        mForegroundOpenTagScanCost = foregroundOpenTagScanCost;
        mBufferReflowCost = bufferReflowCost;
        mSessionReconnectCost = sessionReconnectCost;
        mReplacedSessionShellInput = replacedSessionShellInput;
        mMainThreadStalls = mainThreadStalls;
        mMainLooperQueue = mainLooperQueue;
        mScrollbarViewCensus = scrollbarViewCensus;
        mProcessUptimeMillis = processUptimeMillis;
        mBackgroundCycle = backgroundCycle;
        mVersionChange = versionChange;
        mShellExits = shellExits;
        mPhantomProcessMonitor = phantomProcessMonitor;
        mAppProcessPopulation = appProcessPopulation;
        mTerminalDrawCost = terminalDrawCost;
        mShellOutputParseCost = shellOutputParseCost;
    }

    @NonNull
    public DiagnosticsWorkCostLine getTerminalDrawCost() {
        return mTerminalDrawCost;
    }

    @NonNull
    public DiagnosticsWorkCostLine getShellOutputParseCost() {
        return mShellOutputParseCost;
    }

    @NonNull
    public DiagnosticsSessionCreationPaths getSessionCreationPaths() {
        return mSessionCreationPaths;
    }

    @NonNull
    public DiagnosticsScrollSteps getScrollSteps() {
        return mScrollSteps;
    }

    @NonNull
    public DiagnosticsTouchEvents getTouchEvents() {
        return mTouchEvents;
    }

    @NonNull
    public DiagnosticsPreviousProcessExits getPreviousProcessExits() {
        return mPreviousProcessExits;
    }

    @NonNull
    public DiagnosticsActivityWindows getActivityWindows() {
        return mActivityWindows;
    }

    @NonNull
    public DiagnosticsReportDelivery getLastReportDelivery() {
        return mLastReportDelivery;
    }

    @NonNull
    public DiagnosticsMainLooperQueuePeak getMainLooperQueuePeak() {
        return mMainLooperQueuePeak;
    }

    @NonNull
    public DiagnosticsPhantomProcessMonitor getPhantomProcessMonitor() {
        return mPhantomProcessMonitor;
    }

    @NonNull
    public DiagnosticsAppProcessPopulation getAppProcessPopulation() {
        return mAppProcessPopulation;
    }

    @NonNull
    public DiagnosticsShellExits getShellExits() {
        return mShellExits;
    }

    @NonNull
    public DiagnosticsVersionChange getVersionChange() {
        return mVersionChange;
    }

    @NonNull
    public DiagnosticsBackgroundCycle getBackgroundCycle() {
        return mBackgroundCycle;
    }

    @NonNull
    public DiagnosticsReplacedSessionShellInput getReplacedSessionShellInput() {
        return mReplacedSessionShellInput;
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
    public DiagnosticsWorkCostLine getForegroundOpenTagScanCost() {
        return mForegroundOpenTagScanCost;
    }

    @NonNull
    public DiagnosticsWorkCostLine getBufferReflowCost() {
        return mBufferReflowCost;
    }

    @NonNull
    public DiagnosticsSessionReconnectCost getSessionReconnectCost() {
        return mSessionReconnectCost;
    }

    @NonNull
    public DiagnosticsMainThreadStalls getMainThreadStalls() {
        return mMainThreadStalls;
    }

    @NonNull
    public DiagnosticsMainLooperQueue getMainLooperQueue() {
        return mMainLooperQueue;
    }

    @NonNull
    public ScrollbarViewCensus getScrollbarViewCensus() {
        return mScrollbarViewCensus;
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
