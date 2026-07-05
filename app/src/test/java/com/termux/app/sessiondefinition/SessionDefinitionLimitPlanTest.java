package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class SessionDefinitionLimitPlanTest {

    @Test
    public void createsAllRequestedWhenWithinLimit() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(10, 0, 32);

        Assert.assertEquals(10, plan.getSessionsToCreateCount());
        Assert.assertEquals(0, plan.getDroppedSessionCount());
        Assert.assertFalse(plan.exceedsLimit());
    }

    @Test
    public void dropsSessionsBeyondTheConfiguredLimit() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(40, 0, 32);

        Assert.assertEquals(32, plan.getSessionsToCreateCount());
        Assert.assertEquals(8, plan.getDroppedSessionCount());
        Assert.assertTrue(plan.exceedsLimit());
    }

    @Test
    public void accountsForAlreadyLiveSessionsWhenComputingCapacity() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(20, 30, 32);

        Assert.assertEquals(2, plan.getSessionsToCreateCount());
        Assert.assertEquals(18, plan.getDroppedSessionCount());
        Assert.assertTrue(plan.exceedsLimit());
    }

    @Test
    public void dropsEverythingWhenAlreadyAtLimit() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(5, 32, 32);

        Assert.assertEquals(0, plan.getSessionsToCreateCount());
        Assert.assertEquals(5, plan.getDroppedSessionCount());
        Assert.assertTrue(plan.exceedsLimit());
    }

    @Test
    public void neverProducesNegativeCapacityWhenLiveSessionsExceedLimit() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(5, 40, 32);

        Assert.assertEquals(0, plan.getSessionsToCreateCount());
        Assert.assertEquals(5, plan.getDroppedSessionCount());
        Assert.assertTrue(plan.exceedsLimit());
    }

    @Test
    public void honorsAHigherConfiguredLimit() {
        SessionDefinitionLimitPlan plan = SessionDefinitionLimitPlan.forCapacity(40, 0, 64);

        Assert.assertEquals(40, plan.getSessionsToCreateCount());
        Assert.assertEquals(0, plan.getDroppedSessionCount());
        Assert.assertFalse(plan.exceedsLimit());
    }

    @Test
    public void countSafeReloadOfEveryShownSessionNeverExceedsTheCapWhenPlanningAgainstTheCountedTotal() {
        int configuredLimit = 32;
        int shownSessionCount = 23;
        int countedButNotShownSessionCount = 9;
        int countedLiveTotal = shownSessionCount + countedButNotShownSessionCount;

        SessionDefinitionLimitPlan plan =
            SessionDefinitionLimitPlan.forCapacity(shownSessionCount, countedLiveTotal, configuredLimit);

        int totalAfterCreation = countedLiveTotal + plan.getSessionsToCreateCount();
        Assert.assertTrue(totalAfterCreation <= configuredLimit);
    }

    @Test
    public void countSafeReloadCreatesEveryShownSessionWhenTheCountedTotalLeavesRoom() {
        int configuredLimit = 32;
        int shownSessionCount = 23;
        int countedLiveTotal = 0;

        SessionDefinitionLimitPlan plan =
            SessionDefinitionLimitPlan.forCapacity(shownSessionCount, countedLiveTotal, configuredLimit);

        Assert.assertEquals(shownSessionCount, plan.getSessionsToCreateCount());
        Assert.assertFalse(plan.exceedsLimit());
        Assert.assertTrue(countedLiveTotal + plan.getSessionsToCreateCount() <= configuredLimit);
    }
}
