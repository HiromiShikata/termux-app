package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InitialSessionSelectionTest {

    private static Map<Integer, Boolean> pending(Object... pairs) {
        Map<Integer, Boolean> pendingByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            pendingByIndex.put((Integer) pairs[i], (Boolean) pairs[i + 1]);
        }
        return pendingByIndex;
    }

    @Test
    public void selectsTopmostPendingCallToUserSessionWhenOneExists() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        Map<Integer, Boolean> pendingByIndex = pending(
            1, true,
            3, true);

        int selected = InitialSessionSelection.selectInitialSessionIndex(ordered, pendingByIndex);

        Assert.assertEquals(1, selected);
    }

    @Test
    public void selectsTopmostPendingFollowingDisplayOrderNotIndexValue() {
        List<Integer> ordered = Arrays.asList(3, 2, 1, 0);
        Map<Integer, Boolean> pendingByIndex = pending(
            1, true,
            2, true);

        int selected = InitialSessionSelection.selectInitialSessionIndex(ordered, pendingByIndex);

        Assert.assertEquals(2, selected);
    }

    @Test
    public void selectsTopmostSessionWhenNonePending() {
        List<Integer> ordered = Arrays.asList(4, 5, 6);
        Map<Integer, Boolean> pendingByIndex = pending(
            4, false,
            5, false,
            6, false);

        int selected = InitialSessionSelection.selectInitialSessionIndex(ordered, pendingByIndex);

        Assert.assertEquals(4, selected);
    }

    @Test
    public void selectsTopmostSessionWhenPendingMapIsEmpty() {
        List<Integer> ordered = Arrays.asList(2, 0, 1);

        int selected = InitialSessionSelection.selectInitialSessionIndex(
            ordered, Collections.emptyMap());

        Assert.assertEquals(2, selected);
    }

    @Test
    public void selectsOnlyPendingSessionWhenItIsNotTopmost() {
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        Map<Integer, Boolean> pendingByIndex = pending(
            0, false,
            1, false,
            2, true);

        int selected = InitialSessionSelection.selectInitialSessionIndex(ordered, pendingByIndex);

        Assert.assertEquals(2, selected);
    }

    @Test
    public void returnsNoSelectionWhenNoSessionsExist() {
        int selected = InitialSessionSelection.selectInitialSessionIndex(
            Collections.emptyList(), Collections.emptyMap());

        Assert.assertEquals(-1, selected);
    }
}
