package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class UserRemovedSessionReconnectSuppressionPlannerTest {

    private final UserRemovedSessionReconnectSuppressionPlanner planner =
        new UserRemovedSessionReconnectSuppressionPlanner();

    @Test
    public void suppressesBareLeftoverSessionThatIsNotAlwaysPresent() {
        Assert.assertTrue(planner.shouldSuppressReconnectAfterUserRemoval("google logon"));
    }

    @Test
    public void suppressesAnAlwaysPresentSessionTheOwnerDeleted() {
        Assert.assertTrue("a deletion is a deletion: an always-present session the owner deleted must be"
                + " recorded as removed, so that no restore path creates it again on its own",
            planner.shouldSuppressReconnectAfterUserRemoval("secretary"));
    }

    @Test
    public void doesNotSuppressNullSessionName() {
        Assert.assertFalse(planner.shouldSuppressReconnectAfterUserRemoval(null));
    }

    @Test
    public void doesNotSuppressBlankSessionName() {
        Assert.assertFalse(planner.shouldSuppressReconnectAfterUserRemoval("   "));
    }

    @Test
    public void suppressesUsingTrimmedNameComparison() {
        Assert.assertTrue(planner.shouldSuppressReconnectAfterUserRemoval("  google logon  "));
    }
}
