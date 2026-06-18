package com.termux.app;

public final class BackgroundIdleWakeLockPolicy {

    private boolean mWakeLockAutoReleasedForBackground;

    public synchronized boolean shouldReleaseWakeLockOnBackground(boolean wakeLockCurrentlyHeld) {
        if (!wakeLockCurrentlyHeld) {
            mWakeLockAutoReleasedForBackground = false;
            return false;
        }
        mWakeLockAutoReleasedForBackground = true;
        return true;
    }

    public synchronized boolean shouldReacquireWakeLockOnForeground(boolean wakeLockCurrentlyHeld) {
        if (mWakeLockAutoReleasedForBackground && !wakeLockCurrentlyHeld) {
            mWakeLockAutoReleasedForBackground = false;
            return true;
        }
        mWakeLockAutoReleasedForBackground = false;
        return false;
    }

    public synchronized void onWakeLockManuallyReleased() {
        mWakeLockAutoReleasedForBackground = false;
    }

    public synchronized boolean isWakeLockAutoReleasedForBackground() {
        return mWakeLockAutoReleasedForBackground;
    }
}
