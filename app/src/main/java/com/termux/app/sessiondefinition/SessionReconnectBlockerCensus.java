package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SessionReconnectBlockerCensus {

    public static final SessionReconnectBlockerCensus NOT_TAKEN =
        new SessionReconnectBlockerCensus(false, 0L, Collections.<ConsideredSession>emptyList());

    public static final class ConsideredSession {

        private final boolean mRunning;
        private final boolean mDisplayedRightNow;
        private final boolean mReconnecting;
        private final long mReconnectingForMillis;
        private final boolean mReconnectReportedFailed;
        private final boolean mSilentAndNoLongerReachable;
        private final long mExitBackoffRemainingMillis;
        private final long mSilenceBackoffRemainingMillis;
        private final boolean mPlanned;

        public ConsideredSession(boolean running, boolean displayedRightNow, boolean reconnecting,
                                 long reconnectingForMillis, boolean reconnectReportedFailed,
                                 boolean silentAndNoLongerReachable, long exitBackoffRemainingMillis,
                                 long silenceBackoffRemainingMillis, boolean planned) {
            mRunning = running;
            mDisplayedRightNow = displayedRightNow;
            mReconnecting = reconnecting;
            mReconnectingForMillis = reconnectingForMillis;
            mReconnectReportedFailed = reconnectReportedFailed;
            mSilentAndNoLongerReachable = silentAndNoLongerReachable;
            mExitBackoffRemainingMillis = exitBackoffRemainingMillis;
            mSilenceBackoffRemainingMillis = silenceBackoffRemainingMillis;
            mPlanned = planned;
        }

        boolean isShellGone() {
            return !mRunning;
        }

        boolean isSilentWhileAlive() {
            return mRunning && mSilentAndNoLongerReachable;
        }
    }

    private final boolean mTaken;

    private final long mTakenAtMillis;

    @NonNull
    private final List<ConsideredSession> mConsideredSessions;

    private SessionReconnectBlockerCensus(boolean taken, long takenAtMillis,
                                          @NonNull List<ConsideredSession> consideredSessions) {
        mTaken = taken;
        mTakenAtMillis = takenAtMillis;
        mConsideredSessions = Collections.unmodifiableList(new ArrayList<>(consideredSessions));
    }

    @NonNull
    public static SessionReconnectBlockerCensus of(@NonNull List<ConsideredSession> consideredSessions,
                                                    long takenAtMillis) {
        return new SessionReconnectBlockerCensus(true, takenAtMillis, consideredSessions);
    }

    public boolean isTaken() {
        return mTaken;
    }

    public long getTakenAtMillis() {
        return mTakenAtMillis;
    }

    public int getConsideredCount() {
        return mConsideredSessions.size();
    }

    public int getPlannedCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.mPlanned) count++;
        }
        return count;
    }

    public int getShellGoneCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isShellGone()) count++;
        }
        return count;
    }

    public int getShellGoneMarkedReconnectingCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isShellGone() && consideredSession.mReconnecting) count++;
        }
        return count;
    }

    public long getLongestReconnectingMillis() {
        long longest = 0L;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (!consideredSession.mReconnecting) continue;
            longest = Math.max(longest, consideredSession.mReconnectingForMillis);
        }
        return longest;
    }

    public int getShellGoneInsideTheExitBackoffCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isShellGone() && !consideredSession.mReconnecting
                && consideredSession.mExitBackoffRemainingMillis > 0L) {
                count++;
            }
        }
        return count;
    }

    public long getLongestExitBackoffRemainingMillis() {
        long longest = 0L;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (!consideredSession.isShellGone()) continue;
            longest = Math.max(longest, consideredSession.mExitBackoffRemainingMillis);
        }
        return longest;
    }

    public int getShellGoneReportedFailedCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isShellGone() && consideredSession.mReconnectReportedFailed) count++;
        }
        return count;
    }

    public int getShellGoneReadyButNotPlannedCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isShellGone() && !consideredSession.mReconnecting
                && consideredSession.mExitBackoffRemainingMillis <= 0L
                && !consideredSession.mPlanned) {
                count++;
            }
        }
        return count;
    }

    public int getSilentCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isSilentWhileAlive()) count++;
        }
        return count;
    }

    public int getSilentDisplayedRightNowCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isSilentWhileAlive() && !consideredSession.mReconnecting
                && consideredSession.mDisplayedRightNow) {
                count++;
            }
        }
        return count;
    }

    public int getSilentMarkedReconnectingCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isSilentWhileAlive() && consideredSession.mReconnecting) count++;
        }
        return count;
    }

    public int getSilentInsideTheSilenceBackoffCount() {
        int count = 0;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (consideredSession.isSilentWhileAlive() && !consideredSession.mReconnecting
                && !consideredSession.mDisplayedRightNow
                && consideredSession.mSilenceBackoffRemainingMillis > 0L) {
                count++;
            }
        }
        return count;
    }

    public long getLongestSilenceBackoffRemainingMillis() {
        long longest = 0L;
        for (ConsideredSession consideredSession : mConsideredSessions) {
            if (!consideredSession.isSilentWhileAlive()) continue;
            longest = Math.max(longest, consideredSession.mSilenceBackoffRemainingMillis);
        }
        return longest;
    }
}
