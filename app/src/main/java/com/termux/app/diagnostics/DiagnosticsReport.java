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

    public DiagnosticsReport(@NonNull String versionName, int versionCode, long reportTimestampMillis,
                             int sessionsCountedTowardCap, int sessionsDisplayedCount, int maxSessionsCap,
                             @NonNull List<DiagnosticsSessionLine> sessionLines,
                             int openTabCount, int tabHistoryEntryCount,
                             boolean wakeLockHeld, boolean foreground,
                             @NonNull List<DiagnosticEvent> recentEvents) {
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
}
