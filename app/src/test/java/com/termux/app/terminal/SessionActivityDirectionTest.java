package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionActivityDirectionTest {

    private static Map<Integer, SessionNewActivityTier> tiers(Object... pairs) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            tiersByIndex.put((Integer) pairs[i], (SessionNewActivityTier) pairs[i + 1]);
        }
        return tiersByIndex;
    }

    @Test
    public void noActivityProducesNoTierAndNoDirection() {
        SessionActivityDirection direction =
            SessionActivityDirection.compute(Arrays.asList(0, 1, 2), 1, tiers());

        Assert.assertEquals(SessionNewActivityTier.NONE, direction.getTier());
        Assert.assertFalse(direction.hasActiveAbove());
        Assert.assertFalse(direction.hasActiveBelow());
    }

    @Test
    public void anyRedMakesTheTierRedAndNeverYellow() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3), 1,
            tiers(0, SessionNewActivityTier.YELLOW,
                3, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
    }

    @Test
    public void redArrowLightsUpInTheDirectionWhereARedSessionExists() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4), 2,
            tiers(0, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
        Assert.assertTrue(direction.hasActiveAbove());
        Assert.assertFalse(direction.hasActiveBelow());
    }

    @Test
    public void redArrowsLightBothDirectionsWhenRedSessionsExistAboveAndBelow() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4), 2,
            tiers(0, SessionNewActivityTier.RED,
                4, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
        Assert.assertTrue(direction.hasActiveAbove());
        Assert.assertTrue(direction.hasActiveBelow());
    }

    @Test
    public void yellowDirectionIgnoresYellowSessionsWhenRedExistsElsewhere() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4), 2,
            tiers(0, SessionNewActivityTier.YELLOW,
                4, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
        Assert.assertFalse(direction.hasActiveAbove());
        Assert.assertTrue(direction.hasActiveBelow());
    }

    @Test
    public void yellowTierWhenNoRedAndYellowPerDirection() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4), 2,
            tiers(1, SessionNewActivityTier.YELLOW,
                4, SessionNewActivityTier.YELLOW));

        Assert.assertEquals(SessionNewActivityTier.YELLOW, direction.getTier());
        Assert.assertTrue(direction.hasActiveAbove());
        Assert.assertTrue(direction.hasActiveBelow());
    }

    @Test
    public void currentSessionTierDoesNotContributeToDirection() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(1, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
        Assert.assertFalse(direction.hasActiveAbove());
        Assert.assertFalse(direction.hasActiveBelow());
    }

    @Test
    public void absentCurrentSessionStillResolvesGlobalTierWithoutCrashing() {
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), -1,
            tiers(2, SessionNewActivityTier.RED));

        Assert.assertEquals(SessionNewActivityTier.RED, direction.getTier());
    }

    @Test
    public void emptySessionListProducesNoneTier() {
        SessionActivityDirection direction =
            SessionActivityDirection.compute(Collections.<Integer>emptyList(), -1, tiers());

        Assert.assertEquals(SessionNewActivityTier.NONE, direction.getTier());
    }
}
