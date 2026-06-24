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
public class SessionDefinitionAutoReloadSchedulerTest {

    @Test
    public void millisecondsPerMinuteConstantIsCorrect() {
        Assert.assertEquals(60_000L, SessionDefinitionAutoReloadScheduler.MILLISECONDS_PER_MINUTE);
    }

    @Test
    public void intervalMillisConvertsMinutesToMilliseconds() {
        Assert.assertEquals(0L, SessionDefinitionAutoReloadScheduler.intervalMillis(0));
        Assert.assertEquals(60_000L, SessionDefinitionAutoReloadScheduler.intervalMillis(1));
        Assert.assertEquals(300_000L, SessionDefinitionAutoReloadScheduler.intervalMillis(5));
    }

    @Test
    public void intervalMillisDoesNotOverflowForLargeMinutes() {
        Assert.assertEquals(6_000_000_000L, SessionDefinitionAutoReloadScheduler.intervalMillis(100_000));
    }

    @Test
    public void shouldScheduleOnlyWhenForegroundAndIntervalPositive() {
        Assert.assertTrue(SessionDefinitionAutoReloadScheduler.shouldSchedule(true, 1));
        Assert.assertFalse(SessionDefinitionAutoReloadScheduler.shouldSchedule(true, 0));
        Assert.assertFalse(SessionDefinitionAutoReloadScheduler.shouldSchedule(false, 1));
        Assert.assertFalse(SessionDefinitionAutoReloadScheduler.shouldSchedule(false, 0));
    }

    @Test
    public void zeroIntervalDoesNotSchedule() {
        AtomicInteger reloadCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), reloadCount::incrementAndGet);

        scheduler.onForeground(0);

        Assert.assertFalse(scheduler.isScheduled());
        shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(0, reloadCount.get());
    }

    @Test
    public void foregroundWithPositiveIntervalSchedules() {
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), () -> { });

        scheduler.onForeground(5);

        Assert.assertTrue(scheduler.isScheduled());
    }

    @Test
    public void doubleForegroundDoesNotDoubleSchedule() {
        AtomicInteger reloadCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), reloadCount::incrementAndGet);

        scheduler.onForeground(1);
        scheduler.onForeground(1);

        Assert.assertTrue(scheduler.isScheduled());

        shadowOf(Looper.getMainLooper()).idleFor(60_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, reloadCount.get());
    }

    @Test
    public void tickRunsReloadAndReschedulesWhileForeground() {
        AtomicInteger reloadCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), reloadCount::incrementAndGet);

        scheduler.onForeground(1);
        shadowOf(Looper.getMainLooper()).idleFor(60_000L, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(1, reloadCount.get());
        Assert.assertTrue(scheduler.isScheduled());

        shadowOf(Looper.getMainLooper()).idleFor(60_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, reloadCount.get());
    }

    @Test
    public void backgroundStopsScheduledReload() {
        AtomicInteger reloadCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), reloadCount::incrementAndGet);

        scheduler.onForeground(1);
        scheduler.onBackground();

        Assert.assertFalse(scheduler.isScheduled());
        shadowOf(Looper.getMainLooper()).idleFor(120_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(0, reloadCount.get());
    }

    @Test
    public void backgroundBeforeTickPreventsReload() {
        AtomicInteger reloadCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler =
            new SessionDefinitionAutoReloadScheduler(handler(), reloadCount::incrementAndGet);

        scheduler.onForeground(1);
        shadowOf(Looper.getMainLooper()).idleFor(30_000L, java.util.concurrent.TimeUnit.MILLISECONDS);
        scheduler.onBackground();
        shadowOf(Looper.getMainLooper()).idleFor(120_000L, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, reloadCount.get());
    }

    private static Handler handler() {
        return new Handler(Looper.getMainLooper());
    }
}
