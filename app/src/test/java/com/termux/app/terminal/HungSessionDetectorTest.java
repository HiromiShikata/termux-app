package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HungSessionDetectorTest {

    private static final long NOW_MILLIS = 1_000_000_000_000L;

    private final HungSessionDetector detector = new HungSessionDetector();

    @Test
    public void notHungWhenOutTimeMissing() {
        Assert.assertFalse(detector.isHung(null, NOW_MILLIS));
    }

    @Test
    public void notHungAtExactlyTenMinutes() {
        long exactlyTenMinutesAgo = NOW_MILLIS - HungSessionDetector.STALE_OUT_MAX_AGE_MILLIS;
        Assert.assertFalse(detector.isHung(exactlyTenMinutesAgo, NOW_MILLIS));
    }

    @Test
    public void notHungJustUnderTenMinutes() {
        long justUnder = NOW_MILLIS - (HungSessionDetector.STALE_OUT_MAX_AGE_MILLIS - 1L);
        Assert.assertFalse(detector.isHung(justUnder, NOW_MILLIS));
    }

    @Test
    public void hungJustOverTenMinutes() {
        long justOver = NOW_MILLIS - (HungSessionDetector.STALE_OUT_MAX_AGE_MILLIS + 1L);
        Assert.assertTrue(detector.isHung(justOver, NOW_MILLIS));
    }

    @Test
    public void hungWhenWellStale() {
        long wellStale = NOW_MILLIS - (60L * 60L * 1000L);
        Assert.assertTrue(detector.isHung(wellStale, NOW_MILLIS));
    }
}
