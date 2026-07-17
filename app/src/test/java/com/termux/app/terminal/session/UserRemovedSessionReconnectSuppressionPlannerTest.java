package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserRemovedSessionReconnectSuppressionPlannerTest {

    private final UserRemovedSessionReconnectSuppressionPlanner planner =
        new UserRemovedSessionReconnectSuppressionPlanner();

    @Test
    public void suppressesBareLeftoverSessionThatIsNotAlwaysPresent() {
        Assert.assertTrue(planner.shouldSuppressReconnectAfterUserRemoval(
            "google logon", Collections.emptySet()));
    }

    @Test
    public void doesNotSuppressAlwaysPresentSessionTheOwnerWantsToKeep() {
        Set<String> alwaysPresent = new HashSet<>();
        alwaysPresent.add("secretary");
        Assert.assertFalse(planner.shouldSuppressReconnectAfterUserRemoval("secretary", alwaysPresent));
    }

    @Test
    public void doesNotSuppressNullSessionName() {
        Assert.assertFalse(planner.shouldSuppressReconnectAfterUserRemoval(null, Collections.emptySet()));
    }

    @Test
    public void doesNotSuppressBlankSessionName() {
        Assert.assertFalse(planner.shouldSuppressReconnectAfterUserRemoval("   ", Collections.emptySet()));
    }

    @Test
    public void suppressesUsingTrimmedNameComparison() {
        Assert.assertTrue(planner.shouldSuppressReconnectAfterUserRemoval(
            "  google logon  ", Collections.emptySet()));
    }
}
