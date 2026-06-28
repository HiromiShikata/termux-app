package com.termux.app.sessiondefinition;

import static org.robolectric.Shadows.shadowOf;

import android.os.Handler;
import android.os.Looper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class SessionReconnectSchedulerTest {

    @Test
    public void intervalMillisConvertsMinutesToMilliseconds() {
        Assert.assertEquals(0L, SessionReconnectScheduler.intervalMillis(0));
        Assert.assertEquals(60_000L, SessionReconnectScheduler.intervalMillis(1));
        Assert.assertEquals(300_000L, SessionReconnectScheduler.intervalMillis(5));
    }

    @Test
    public void shouldScheduleOnlyWhenForegroundAndIntervalPositive() {
        Assert.assertTrue(SessionReconnectScheduler.shouldSchedule(true, 1));
        Assert.assertFalse(SessionReconnectScheduler.shouldSchedule(true, 0));
        Assert.assertFalse(SessionReconnectScheduler.shouldSchedule(false, 1));
        Assert.assertFalse(SessionReconnectScheduler.shouldSchedule(false, 0));
    }

    @Test
    public void zeroIntervalDoesNotSchedule() {
        AtomicInteger reconnectCount = new AtomicInteger();
        SessionReconnectScheduler scheduler =
            new SessionReconnectScheduler(handler(), reconnectCount::incrementAndGet);

        scheduler.onForeground(0);

        Assert.assertFalse(scheduler.isScheduled());
        shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(0, reconnectCount.get());
    }

    @Test
    public void foregroundWithPositiveIntervalSchedules() {
        SessionReconnectScheduler scheduler =
            new SessionReconnectScheduler(handler(), () -> { });

        scheduler.onForeground(5);

        Assert.assertTrue(scheduler.isScheduled());
    }

    @Test
    public void tickRunsReconnectAndReschedulesWhileForeground() {
        AtomicInteger reconnectCount = new AtomicInteger();
        SessionReconnectScheduler scheduler =
            new SessionReconnectScheduler(handler(), reconnectCount::incrementAndGet);

        scheduler.onForeground(1);
        shadowOf(Looper.getMainLooper()).idleFor(60_000L, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(1, reconnectCount.get());
        Assert.assertTrue(scheduler.isScheduled());

        shadowOf(Looper.getMainLooper()).idleFor(60_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, reconnectCount.get());
    }

    @Test
    public void backgroundStopsScheduledReconnect() {
        AtomicInteger reconnectCount = new AtomicInteger();
        SessionReconnectScheduler scheduler =
            new SessionReconnectScheduler(handler(), reconnectCount::incrementAndGet);

        scheduler.onForeground(1);
        scheduler.onBackground();

        Assert.assertFalse(scheduler.isScheduled());
        shadowOf(Looper.getMainLooper()).idleFor(120_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(0, reconnectCount.get());
    }

    @Test
    public void backgroundBeforeTickPreventsReconnect() {
        AtomicInteger reconnectCount = new AtomicInteger();
        SessionReconnectScheduler scheduler =
            new SessionReconnectScheduler(handler(), reconnectCount::incrementAndGet);

        scheduler.onForeground(1);
        shadowOf(Looper.getMainLooper()).idleFor(30_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        scheduler.onBackground();
        shadowOf(Looper.getMainLooper()).idleFor(120_000L, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, reconnectCount.get());
    }

    private static Handler handler() {
        return new Handler(Looper.getMainLooper());
    }
}
