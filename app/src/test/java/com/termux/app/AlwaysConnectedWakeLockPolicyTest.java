package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysConnectedWakeLockPolicyTest {

    @Test
    public void acquiresWhenFirstDefinitionBackedSessionBecomesActiveAndLockNotHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
    }

    @Test
    public void doesNothingWhenLockAlreadyHeldAndSessionsActive() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(2, true, true));
    }

    @Test
    public void releasesWhenNoSessionActiveAndLockHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(0, true, true));
    }

    @Test
    public void doesNothingWhenNoSessionActiveAndLockNotHeld() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false, true));
    }

    @Test
    public void releasesHeldLockWhenBackgroundedEvenWithActiveSessions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(28, true, false));
    }

    @Test
    public void doesNotAcquireWhileBackgroundedEvenWithActiveSessions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(28, false, false));
    }

    @Test
    public void reacquiresOnForegroundWhenSessionsActiveAfterBackgroundRelease() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(1, true, false));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
    }

    @Test
    public void acquireIsIdempotentAcrossRepeatedDecisions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, true, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(3, true, true));
    }

    @Test
    public void releaseIsIdempotentAcrossRepeatedDecisions() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.RELEASE, policy.decide(0, true, true));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false, true));
    }

    @Test
    public void manualReleaseSuppressesReacquireWhileSessionsStayActive() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.decide(1, false, true);
        policy.onWakeLockManuallyReleased(1);

        Assert.assertTrue(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, false, true));
    }

    @Test
    public void manualReleaseSuppressionSurvivesBackgroundForegroundCycle() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.decide(1, false, true);
        policy.onWakeLockManuallyReleased(1);

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, false, false));
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, false, true));
        Assert.assertTrue(policy.isManuallyReleasedWhileSessionsActive());
    }

    @Test
    public void manualReleaseSuppressionClearsWhenActiveCountReturnsToZero() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(2);
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(0, false, true));
        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());

        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
    }

    @Test
    public void manualAcquireClearsSuppression() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(1);
        Assert.assertTrue(policy.isManuallyReleasedWhileSessionsActive());

        policy.onWakeLockManuallyAcquired();
        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.NONE, policy.decide(1, true, true));
    }

    @Test
    public void manualReleaseWithNoActiveSessionsDoesNotSuppress() {
        AlwaysConnectedWakeLockPolicy policy = new AlwaysConnectedWakeLockPolicy();

        policy.onWakeLockManuallyReleased(0);

        Assert.assertFalse(policy.isManuallyReleasedWhileSessionsActive());
        Assert.assertEquals(AlwaysConnectedWakeLockPolicy.Decision.ACQUIRE, policy.decide(1, false, true));
    }
}
