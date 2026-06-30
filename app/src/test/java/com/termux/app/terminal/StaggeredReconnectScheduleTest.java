package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class StaggeredReconnectScheduleTest {

    @Test
    public void aSingleConcurrentWindowStartsTheFirstReconnectImmediatelyAndSpacesTheRest() {
        StaggeredReconnectSchedule schedule = new StaggeredReconnectSchedule(400L, 1);

        Assert.assertEquals(0L, schedule.startDelayMillisForIndex(0));
        Assert.assertEquals(400L, schedule.startDelayMillisForIndex(1));
        Assert.assertEquals(800L, schedule.startDelayMillisForIndex(2));
        Assert.assertEquals(1200L, schedule.startDelayMillisForIndex(3));
    }

    @Test
    public void aConcurrentWindowStartsThatManyImmediatelyThenSpacesEachWindow() {
        StaggeredReconnectSchedule schedule = new StaggeredReconnectSchedule(500L, 2);

        Assert.assertEquals(0L, schedule.startDelayMillisForIndex(0));
        Assert.assertEquals(0L, schedule.startDelayMillisForIndex(1));
        Assert.assertEquals(500L, schedule.startDelayMillisForIndex(2));
        Assert.assertEquals(500L, schedule.startDelayMillisForIndex(3));
        Assert.assertEquals(1000L, schedule.startDelayMillisForIndex(4));
    }

    @Test
    public void aNonPositiveConcurrentWindowIsTreatedAsAtLeastOne() {
        StaggeredReconnectSchedule schedule = new StaggeredReconnectSchedule(300L, 0);

        Assert.assertEquals(0L, schedule.startDelayMillisForIndex(0));
        Assert.assertEquals(300L, schedule.startDelayMillisForIndex(1));
    }
}
