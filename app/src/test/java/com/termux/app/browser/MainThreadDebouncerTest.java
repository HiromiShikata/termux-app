package com.termux.app.browser;

import android.os.Handler;
import android.os.Looper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class MainThreadDebouncerTest {

    private static final long DELAY_MS = 500L;

    private Handler mHandler;
    private ShadowLooper mShadowLooper;
    private MainThreadDebouncer mDebouncer;

    @Before
    public void setUp() {
        mHandler = new Handler(Looper.getMainLooper());
        mShadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        mDebouncer = new MainThreadDebouncer(mHandler, DELAY_MS);
    }

    @Test
    public void scheduledTaskRunsAfterDelay() {
        AtomicInteger runCount = new AtomicInteger();
        mDebouncer.schedule(runCount::incrementAndGet);

        Assert.assertEquals(0, runCount.get());
        mShadowLooper.idleFor(DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        Assert.assertEquals(1, runCount.get());
    }

    @Test
    public void reschedulingCancelsPreviousTask() {
        AtomicInteger firstRunCount = new AtomicInteger();
        AtomicInteger secondRunCount = new AtomicInteger();

        mDebouncer.schedule(firstRunCount::incrementAndGet);
        mShadowLooper.idleFor(DELAY_MS / 2, java.util.concurrent.TimeUnit.MILLISECONDS);
        mDebouncer.schedule(secondRunCount::incrementAndGet);
        mShadowLooper.idleFor(DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, firstRunCount.get());
        Assert.assertEquals(1, secondRunCount.get());
    }

    @Test
    public void cancelPreventsScheduledTask() {
        AtomicInteger runCount = new AtomicInteger();
        mDebouncer.schedule(runCount::incrementAndGet);
        mDebouncer.cancel();
        mShadowLooper.idleFor(DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, runCount.get());
    }
}
