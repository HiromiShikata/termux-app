package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HungSessionDetectorTest {

    private static final long NOW_MILLIS = 1_000_000_000_000L;

    private static final long FRESH_OUT_MILLIS = NOW_MILLIS - (5L * 60L * 1000L);

    private static final long STALE_OUT_MILLIS = NOW_MILLIS - (11L * 60L * 1000L);

    private final HungSessionDetector detector = new HungSessionDetector();

    @Test
    public void hungWhenOutOlderThanTenMinutes() {
        Assert.assertTrue(detector.isHungConnection(STALE_OUT_MILLIS, NOW_MILLIS));
    }

    @Test
    public void notHungWhenOutWithinTenMinutes() {
        Assert.assertFalse(detector.isHungConnection(FRESH_OUT_MILLIS, NOW_MILLIS));
    }

    @Test
    public void notHungAtExactlyTenMinutes() {
        long exactlyTenMinutesAgo = NOW_MILLIS - (10L * 60L * 1000L);
        Assert.assertFalse(detector.isHungConnection(exactlyTenMinutesAgo, NOW_MILLIS));
    }

    @Test
    public void notHungWhenOutTimeMissing() {
        Assert.assertFalse(detector.isHungConnection(null, NOW_MILLIS));
    }
}
