package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SafeLinearLayoutManagerTest {

    @Test
    public void itemPrefetchIsDisabledSoTheGapWorkerUnhidePrefetchCrashCannotOccur() {
        SafeLinearLayoutManager layoutManager =
            new SafeLinearLayoutManager(RuntimeEnvironment.getApplication());

        Assert.assertFalse("item prefetch must be disabled so the RecyclerView GapWorker does not run the "
                + "prefetch path that throws \"view is not a child, cannot hide\" during the empty-session "
                + "transition",
            layoutManager.isItemPrefetchEnabled());
    }

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
