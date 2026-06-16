package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VisibleSessionNavigatorTest {

    @Test
    public void returnsNegativeOneWhenNothingIsNavigable() {
        Assert.assertEquals(-1, VisibleSessionNavigator.nextSessionIndex(
            Arrays.asList(0, 1, 2), Collections.emptyList(), 0, true));
        Assert.assertEquals(-1, VisibleSessionNavigator.nextSessionIndex(
            Arrays.asList(0, 1, 2), Collections.emptyList(), 0, false));
    }

    @Test
    public void stepsForwardWithinNavigableSet() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, true));
        Assert.assertEquals(2, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
    }

    @Test
    public void stepsBackwardWithinNavigableSet() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, false));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, false));
    }

    @Test
    public void wrapsForwardAtTheEndOfNavigableSet() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
    }

    @Test
    public void wrapsBackwardAtTheStartOfNavigableSet() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        Assert.assertEquals(2, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, false));
    }

    @Test
    public void navigatesInDisplayOrderWhenNavigableIndexesAreNotSortedAscending() {
        List<Integer> ordered = Arrays.asList(2, 3, 0, 1);
        List<Integer> navigable = Arrays.asList(2, 3, 0, 1);

        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, false));
    }

    @Test
    public void crossingCollapsedBoundaryFromFilteredCurrentAdvancesExactlyOneInDisplayOrderForward() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4, 5);
        List<Integer> navigable = Arrays.asList(0, 1, 4, 5);

        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
    }

    @Test
    public void crossingCollapsedBoundaryFromFilteredCurrentAdvancesExactlyOneInDisplayOrderBackward() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4, 5);
        List<Integer> navigable = Arrays.asList(0, 1, 4, 5);

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, false));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, false));
    }

    @Test
    public void filteredCurrentUsesDisplayPositionNotNumericNearest() {
        List<Integer> ordered = Arrays.asList(4, 5, 6, 0, 1, 2);
        List<Integer> navigable = Arrays.asList(4, 5, 0, 1, 2);

        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 6, true));
        Assert.assertEquals(5, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 6, false));
    }

    @Test
    public void filteredCurrentNearCollapsedBottomBoundaryDoesNotRepeatForward() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = Arrays.asList(0, 1);

        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
    }

    @Test
    public void filteredCurrentNearCollapsedTopBoundaryDoesNotRepeatBackward() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = Arrays.asList(2, 3);

        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, false));
        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, false));
    }

    @Test
    public void disabledSessionAdjacentToCollapsedBoundaryIsSkipped() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 4);

        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, true));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 4, true));
        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, false));
    }

    @Test
    public void soleNavigableSessionIsAlwaysReturned() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Collections.singletonList(1);

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, false));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, false));
    }

    @Test
    public void currentNotInOrderedFallsBackToFirstNavigableForwardAndLastBackward() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 99, true));
        Assert.assertEquals(2, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 99, false));
    }
}
