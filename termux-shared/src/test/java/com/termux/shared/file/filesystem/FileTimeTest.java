package com.termux.shared.file.filesystem;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class FileTimeTest {

    @Test
    public void fromMillisStoresValueInMilliseconds() {
        FileTime fileTime = FileTime.fromMillis(1500L);
        Assert.assertEquals(1500L, fileTime.toMillis());
    }

    @Test
    public void fromMillisSupportsNegativeValues() {
        FileTime fileTime = FileTime.fromMillis(-2000L);
        Assert.assertEquals(-2000L, fileTime.toMillis());
    }

    @Test
    public void fromSecondsConvertsToMillis() {
        FileTime fileTime = FileTime.from(5L, TimeUnit.SECONDS);
        Assert.assertEquals(5000L, fileTime.toMillis());
    }

    @Test
    public void toConvertsSecondsToMilliseconds() {
        FileTime fileTime = FileTime.from(3L, TimeUnit.SECONDS);
        Assert.assertEquals(3000L, fileTime.to(TimeUnit.MILLISECONDS));
    }

    @Test
    public void toConvertsMillisecondsDownToSecondsTruncating() {
        FileTime fileTime = FileTime.fromMillis(2999L);
        Assert.assertEquals(2L, fileTime.to(TimeUnit.SECONDS));
    }

    @Test
    public void toReturnsSameValueWhenUnitsMatch() {
        FileTime fileTime = FileTime.from(7L, TimeUnit.MINUTES);
        Assert.assertEquals(7L, fileTime.to(TimeUnit.MINUTES));
    }

    @Test(expected = NullPointerException.class)
    public void fromRejectsNullUnit() {
        FileTime.from(1L, null);
    }

    @Test(expected = NullPointerException.class)
    public void toRejectsNullUnit() {
        FileTime.fromMillis(1L).to(null);
    }

    @Test
    public void getDateFormatsEpochMillisWithGivenPattern() {
        Assert.assertEquals("1970-01-01", FileTime.getDate(0L, "yyyy-MM-dd"));
    }

    @Test
    public void getDateFallsBackToRawMillisStringWhenFormatIsInvalid() {
        Assert.assertEquals("0", FileTime.getDate(0L, "not-a-valid-pattern-yyyy-Q-Q-Q-VVVV-{}"));
    }

    @Test
    public void toStringIsNonNullAndContainsFormattedTime() {
        String rendered = FileTime.fromMillis(0L).toString();
        Assert.assertNotNull(rendered);
        Assert.assertTrue(rendered.contains("1970"));
    }
}
