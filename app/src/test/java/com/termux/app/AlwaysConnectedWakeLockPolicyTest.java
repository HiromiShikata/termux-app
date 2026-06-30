package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysConnectedWakeLockPolicyTest {

    @Test
    public void acquiresWhenFirstDefinitionBackedSessionBecomesActiveAndLockNotHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false));
    }

    @Test
    public void doesNothingWhenLockAlreadyHeldAndSessionsActive() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(2, true));
    }

    @Test
    public void releasesWhenNoSessionActiveAndLockHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(0, true));
    }

    @Test
    public void doesNothingWhenNoSessionActiveAndLockNotHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false));
    }

    @Test
    public void acquireIsIdempotentAcrossRepeatedDecisions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(3, true));
    }

    @Test
    public void releaseIsIdempotentAcrossRepeatedDecisions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(0, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false));
    }

    @Test
    public void manualReleaseSuppressesReacquireWhileSessionsStayActive() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.decide(1, false);
        policy.onWakeLockManuallyReleased(1);

        Assert.assertTrue(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, false));
    }

    @Test
    public void manualReleaseSuppressionClearsWhenActiveCountReturnsToZero() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(2);
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false));
        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false));
    }

    @Test
    public void manualAcquireClearsSuppression() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(1);
        Assert.assertTrue(policy.isManuallyReleasedWhileSessionsActive());

        policy.onWakeLockManuallyAcquired();
        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, true));
    }

    @Test
    public void manualReleaseWithNoActiveSessionsDoesNotSuppress() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(0);

        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false));
    }
}
