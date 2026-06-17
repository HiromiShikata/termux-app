package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

public class BackgroundIdleWakeLockPolicyTest {

    @Test
    public void releasesHeldWakeLockWhenBackgroundedAndReacquiresWhenForegrounded() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        Assert.assertTrue(policy.shouldReleaseWakeLockOnBackground(true));
        Assert.assertTrue(policy.isWakeLockAutoReleasedForBackground());
        Assert.assertTrue(policy.shouldReacquireWakeLockOnForeground(false));
        Assert.assertFalse(policy.isWakeLockAutoReleasedForBackground());
    }

    @Test
    public void doesNotReleaseWhenNoWakeLockHeld() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        Assert.assertFalse(policy.shouldReleaseWakeLockOnBackground(false));
        Assert.assertFalse(policy.isWakeLockAutoReleasedForBackground());
    }

    @Test
    public void doesNotReacquireWhenWakeLockWasNeverHeld() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        policy.shouldReleaseWakeLockOnBackground(false);

        Assert.assertFalse(policy.shouldReacquireWakeLockOnForeground(false));
    }

    @Test
    public void doesNotReacquireWhenWakeLockIsAlreadyHeldOnForeground() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        policy.shouldReleaseWakeLockOnBackground(true);

        Assert.assertFalse(policy.shouldReacquireWakeLockOnForeground(true));
        Assert.assertFalse(policy.isWakeLockAutoReleasedForBackground());
    }

    @Test
    public void manualReleaseClearsPendingReacquire() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        policy.shouldReleaseWakeLockOnBackground(true);
        policy.onWakeLockManuallyReleased();

        Assert.assertFalse(policy.isWakeLockAutoReleasedForBackground());
        Assert.assertFalse(policy.shouldReacquireWakeLockOnForeground(false));
    }

    @Test
    public void backgroundingWithoutHeldWakeLockClearsPriorAutoReleaseFlag() {
        BackgroundIdleWakeLockPolicy policy = new BackgroundIdleWakeLockPolicy();

        policy.shouldReleaseWakeLockOnBackground(true);
        Assert.assertFalse(policy.shouldReleaseWakeLockOnBackground(false));

        Assert.assertFalse(policy.isWakeLockAutoReleasedForBackground());
        Assert.assertFalse(policy.shouldReacquireWakeLockOnForeground(false));
    }
}
