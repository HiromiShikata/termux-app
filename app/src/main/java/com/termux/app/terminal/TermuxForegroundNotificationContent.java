package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TermuxForegroundNotificationContent {

    private final int mSessionCount;
    private final int mTaskCount;
    private final int mPendingCallSessionCount;
    private final boolean mWakeLockHeld;

    public TermuxForegroundNotificationContent(int sessionCount, int taskCount,
                                               int pendingCallSessionCount, boolean wakeLockHeld) {
        mSessionCount = sessionCount;
        mTaskCount = taskCount;
        mPendingCallSessionCount = pendingCallSessionCount;
        mWakeLockHeld = wakeLockHeld;
    }

    @NonNull
    public String getText() {
        StringBuilder text = new StringBuilder();
        text.append(mSessionCount).append(" session").append(mSessionCount == 1 ? "" : "s");
        if (mTaskCount > 0) {
            text.append(", ").append(mTaskCount).append(" task").append(mTaskCount == 1 ? "" : "s");
        }
        String pendingCallFractionSuffix =
            PendingCallNotificationText.fractionSuffix(mPendingCallSessionCount, mSessionCount);
        if (!pendingCallFractionSuffix.isEmpty()) {
            text.append(", ").append(pendingCallFractionSuffix);
        }
        if (mWakeLockHeld) {
            text.append(" (wake lock held)");
        }
        return text.toString();
    }

    public boolean isWakeLockHeld() {
        return mWakeLockHeld;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TermuxForegroundNotificationContent)) {
            return false;
        }
        TermuxForegroundNotificationContent otherContent = (TermuxForegroundNotificationContent) other;
        return mSessionCount == otherContent.mSessionCount
            && mTaskCount == otherContent.mTaskCount
            && mPendingCallSessionCount == otherContent.mPendingCallSessionCount
            && mWakeLockHeld == otherContent.mWakeLockHeld;
    }

    @Override
    public int hashCode() {
        int result = mSessionCount;
        result = 31 * result + mTaskCount;
        result = 31 * result + mPendingCallSessionCount;
        result = 31 * result + (mWakeLockHeld ? 1 : 0);
        return result;
    }
}
