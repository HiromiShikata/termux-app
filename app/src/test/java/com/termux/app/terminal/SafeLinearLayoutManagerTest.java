package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SafeLinearLayoutManagerTest {

    @Test
    public void layoutPassThatThrowsInconsistencyIsSwallowedSoTheActivityDoesNotCrash() {
        boolean[] ran = {false};

        SafeLinearLayoutManager.layoutChildrenSwallowingInconsistency(() -> {
            ran[0] = true;
            throw new IndexOutOfBoundsException(
                "Inconsistency detected. Invalid item position 7(offset:-1).state:27");
        });

        Assert.assertTrue("the layout pass must still be attempted", ran[0]);
    }

    @Test
    public void successfulLayoutPassRunsNormallyWithoutInterference() {
        int[] runCount = {0};

        SafeLinearLayoutManager.layoutChildrenSwallowingInconsistency(() -> runCount[0]++);

        Assert.assertEquals(1, runCount[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void unrelatedRuntimeExceptionIsNotSwallowedSoGenuineBugsStaySurfaced() {
        SafeLinearLayoutManager.layoutChildrenSwallowingInconsistency(() -> {
            throw new IllegalStateException("unrelated failure that must propagate");
        });
    }
}
