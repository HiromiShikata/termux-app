package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionBellDirectionTest {

    private static final List<Integer> ORDERED = Arrays.asList(10, 11, 12, 13, 14);

    private static Set<Integer> bellsOn(Integer... sessionIndexes) {
        return new LinkedHashSet<>(Arrays.asList(sessionIndexes));
    }

    @Test
    public void bellOnlyAboveCurrentGlowsUpNotDown() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 12, bellsOn(10, 11));

        Assert.assertTrue(direction.hasBellAbove());
        Assert.assertFalse(direction.hasBellBelow());
    }

    @Test
    public void bellOnlyBelowCurrentGlowsDownNotUp() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 12, bellsOn(13, 14));

        Assert.assertFalse(direction.hasBellAbove());
        Assert.assertTrue(direction.hasBellBelow());
    }

    @Test
    public void bellsOnBothSidesGlowBothArrows() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 12, bellsOn(10, 14));

        Assert.assertTrue(direction.hasBellAbove());
        Assert.assertTrue(direction.hasBellBelow());
    }

    @Test
    public void noUnseenBellsGlowsNeitherArrow() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 12, Collections.emptySet());

        Assert.assertFalse(direction.hasBellAbove());
        Assert.assertFalse(direction.hasBellBelow());
    }

    @Test
    public void bellOnCurrentSessionOnlyGlowsNeitherArrow() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 12, bellsOn(12));

        Assert.assertFalse(direction.hasBellAbove());
        Assert.assertFalse(direction.hasBellBelow());
    }

    @Test
    public void currentSessionNotInOrderGlowsNeitherArrow() {
        SessionBellDirection direction = SessionBellDirection.compute(ORDERED, 99, bellsOn(10, 14));

        Assert.assertFalse(direction.hasBellAbove());
        Assert.assertFalse(direction.hasBellBelow());
    }

    @Test
    public void usesDisplayOrderNotNumericSessionIndex() {
        List<Integer> reverseOrdered = Arrays.asList(14, 13, 12, 11, 10);
        SessionBellDirection direction = SessionBellDirection.compute(reverseOrdered, 12, bellsOn(14));

        Assert.assertTrue(direction.hasBellAbove());
        Assert.assertFalse(direction.hasBellBelow());
    }
}
