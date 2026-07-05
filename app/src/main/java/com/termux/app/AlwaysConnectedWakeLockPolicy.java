package com.termux.app;

public final class AlwaysConnectedWakeLockPolicy {

    public enum Decision {
        ACQUIRE,
        RELEASE,
        NONE
    }

    private boolean mManuallyReleasedWhileSessionsActive;

    public synchronized Decision decide(int activeDefinitionBackedSessionCount, boolean wakeLockCurrentlyHeld, boolean activityInForeground) {
        if (!activityInForeground) {
            return wakeLockCurrentlyHeld ? Decision.RELEASE : Decision.NONE;
        }
        if (activeDefinitionBackedSessionCount <= 0) {
            mManuallyReleasedWhileSessionsActive = false;
            return wakeLockCurrentlyHeld ? Decision.RELEASE : Decision.NONE;
        }
        if (mManuallyReleasedWhileSessionsActive) {
            return Decision.NONE;
        }
        return wakeLockCurrentlyHeld ? Decision.NONE : Decision.ACQUIRE;
    }

    public synchronized void onWakeLockManuallyReleased(int activeDefinitionBackedSessionCount) {
        mManuallyReleasedWhileSessionsActive = activeDefinitionBackedSessionCount > 0;
    }

    public synchronized void onWakeLockManuallyAcquired() {
        mManuallyReleasedWhileSessionsActive = false;
    }

    public synchronized boolean isManuallyReleasedWhileSessionsActive() {
        return mManuallyReleasedWhileSessionsActive;
    }
}
