package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserFindMatchCounterTest {

    @Test
    public void fromListenerResultConvertsZeroBasedOrdinalToOneBased() {
        BrowserFindMatchCounter counter = BrowserFindMatchCounter.fromListenerResult(2, 12);

        Assert.assertEquals(3, counter.getActiveMatchOrdinal());
        Assert.assertEquals(12, counter.getNumberOfMatches());
        Assert.assertEquals("3/12", counter.formatForQuery("term"));
    }

    @Test
    public void fromListenerResultWithNoMatchesIsEmpty() {
        BrowserFindMatchCounter counter = BrowserFindMatchCounter.fromListenerResult(-1, 0);

        Assert.assertFalse(counter.hasMatches());
        Assert.assertEquals("0/0", counter.formatForQuery("term"));
    }

    @Test
    public void formatForBlankQueryIsEmptyString() {
        BrowserFindMatchCounter counter = BrowserFindMatchCounter.fromListenerResult(0, 5);

        Assert.assertEquals("", counter.formatForQuery("   "));
        Assert.assertEquals("", counter.formatForQuery(""));
    }

    @Test
    public void firstOfSingleMatchFormatsAsOneOverOne() {
        BrowserFindMatchCounter counter = BrowserFindMatchCounter.fromListenerResult(0, 1);

        Assert.assertEquals("1/1", counter.formatForQuery("q"));
    }

    @Test
    public void negativeInputsAreClampedToZero() {
        BrowserFindMatchCounter counter = new BrowserFindMatchCounter(-5, -3);

        Assert.assertEquals(0, counter.getActiveMatchOrdinal());
        Assert.assertEquals(0, counter.getNumberOfMatches());
        Assert.assertFalse(counter.hasMatches());
    }
}
