package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Locale;

public class ApkUpdateCheckTimeFormatterTest {

    private final ApkUpdateCheckTimeFormatter formatter = new ApkUpdateCheckTimeFormatter();

    @Test
    public void formatsEpochMillisInUtcUsingReadablePattern() {
        String formatted = formatter.format(1_000_000_000_000L, ZoneId.of("UTC"), Locale.US);

        Assert.assertEquals("2001-09-09 01:46", formatted);
    }

    @Test
    public void formatsEpochMillisHonouringRequestedZone() {
        String formatted = formatter.format(1_000_000_000_000L, ZoneId.of("Asia/Tokyo"), Locale.US);

        Assert.assertEquals("2001-09-09 10:46", formatted);
    }

    @Test
    public void hasCheckTimeIsFalseWhenNeverChecked() {
        Assert.assertFalse(formatter.hasCheckTime(ApkUpdateManager.NO_CHECK_TIME));
    }

    @Test
    public void hasCheckTimeIsTrueForRecordedTimestamp() {
        Assert.assertTrue(formatter.hasCheckTime(1_000_000_000_000L));
    }
}
