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

    @Test
    public void defaultConstructorUsesTheTenMinuteHungThreshold() {
        Assert.assertEquals(HungSessionDetector.STALE_OUT_MAX_AGE_MILLIS, detector.getStaleOutMaxAgeMillis());
    }

    @Test
    public void aCustomThresholdIsHonoredSoTheBackgroundReconnectCanUseAFresherStalenessWindow() {
        long fourMinutes = 4L * 60L * 1000L;
        HungSessionDetector fresherDetector = new HungSessionDetector(fourMinutes);

        long justUnderFourMinutes = NOW_MILLIS - (fourMinutes - 1L);
        long justOverFourMinutes = NOW_MILLIS - (fourMinutes + 1L);

        Assert.assertEquals(fourMinutes, fresherDetector.getStaleOutMaxAgeMillis());
        Assert.assertFalse(fresherDetector.isHung(justUnderFourMinutes, NOW_MILLIS));
        Assert.assertTrue(fresherDetector.isHung(justOverFourMinutes, NOW_MILLIS));
    }

    @Test
    public void aFreshOutputSessionIsNeverStaleUnderTheCustomThreshold() {
        HungSessionDetector fresherDetector = new HungSessionDetector(4L * 60L * 1000L);
        long tenSecondsAgo = NOW_MILLIS - (10L * 1000L);
        Assert.assertFalse(fresherDetector.isHung(tenSecondsAgo, NOW_MILLIS));
    }
}
