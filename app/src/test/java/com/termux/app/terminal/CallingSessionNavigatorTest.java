package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CallingSessionNavigatorTest {

    private static Set<String> callingNames(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    @Test
    public void returnsNegativeOneWhenNoSessionIsCalling() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<String> names = Arrays.asList("alpha", "beta", "gamma");

        Assert.assertEquals(-1, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, Collections.emptySet()));
    }

    @Test
    public void returnsTopmostCallingSessionIndexInDisplayOrder() {
        List<Integer> ordered = Arrays.asList(2, 0, 1);
        List<String> names = Arrays.asList("alpha", "beta", "gamma");

        Assert.assertEquals(2, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, callingNames("alpha", "gamma")));
    }

    @Test
    public void returnsFirstCallingSessionInDisplayOrderNotSmallestIndex() {
        List<Integer> ordered = Arrays.asList(2, 1, 0);
        List<String> names = Arrays.asList("alpha", "beta", "gamma");

        Assert.assertEquals(1, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, callingNames("alpha", "beta")));
    }

    @Test
    public void ignoresCallingNamesThatAreNotInTheSessionList() {
        List<Integer> ordered = Arrays.asList(0, 1);
        List<String> names = Arrays.asList("alpha", "beta");

        Assert.assertEquals(-1, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, callingNames("gamma")));
    }

    @Test
    public void topmostSkipsSessionsWithNullNames() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<String> names = Arrays.asList("alpha", null, "gamma");

        Assert.assertEquals(2, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, callingNames("gamma")));
    }

    @Test
    public void topmostIgnoresIndexesOutOfRange() {
        List<Integer> ordered = Arrays.asList(5, 0);
        List<String> names = Arrays.asList("alpha", "beta");

        Assert.assertEquals(0, CallingSessionNavigator.topmostCallingSessionIndex(
            ordered, names, callingNames("alpha")));
    }

    @Test
    public void countIsZeroWhenNoSessionIsCalling() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<String> names = Arrays.asList("alpha", "beta", "gamma");

        Assert.assertEquals(0, CallingSessionNavigator.callingSessionCount(
            ordered, names, Collections.emptySet()));
    }

    @Test
    public void countReflectsNumberOfCallingSessions() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        List<String> names = Arrays.asList("alpha", "beta", "gamma");

        Assert.assertEquals(2, CallingSessionNavigator.callingSessionCount(
            ordered, names, callingNames("alpha", "gamma")));
    }

    @Test
    public void countIgnoresCallingNamesNotPresentInSessionList() {
        List<Integer> ordered = Arrays.asList(0, 1);
        List<String> names = Arrays.asList("alpha", "beta");

        Assert.assertEquals(1, CallingSessionNavigator.callingSessionCount(
            ordered, names, new HashSet<>(Arrays.asList("alpha", "gamma"))));
    }

    @Test
    public void countSkipsIndexesOutOfRangeAndNullNames() {
        List<Integer> ordered = Arrays.asList(0, 1, 9);
        List<String> names = Arrays.asList("alpha", null, "gamma");

        Assert.assertEquals(1, CallingSessionNavigator.callingSessionCount(
            ordered, names, callingNames("alpha", "gamma")));
    }
}
