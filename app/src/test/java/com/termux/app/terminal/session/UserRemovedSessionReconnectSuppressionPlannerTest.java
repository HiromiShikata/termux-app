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

    @Test
    public void aHostSessionKillLeavesAnAlwaysPresentSessionNameCreatedAutomaticallyAsBefore() {
        Set<String> alwaysPresent = new HashSet<>();
        alwaysPresent.add("secretary");
        Assert.assertFalse("killing the host session tears the local session down so that it can be "
                + "established again, so an always-present session name must keep being created "
                + "automatically after a host session kill",
            planner.shouldSuppressReconnectAfterHostSessionKill("secretary", alwaysPresent));
    }

    @Test
    public void aHostSessionKillStillSuppressesASessionNameThatIsNotAlwaysPresent() {
        Assert.assertTrue(planner.shouldSuppressReconnectAfterHostSessionKill(
            "google logon", Collections.emptySet()));
    }

    @Test
    public void aHostSessionKillSuppressesNothingForABlankSessionName() {
        Assert.assertFalse(planner.shouldSuppressReconnectAfterHostSessionKill(
            "   ", Collections.emptySet()));
    }
}
