package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPasskeyHintDebounceTest {

    @Test
    public void showsOnTheFirstDetection() {
        BrowserPasskeyHintDebounce debounce = new BrowserPasskeyHintDebounce(10_000L);

        Assert.assertTrue(debounce.shouldShow(0L));
    }

    @Test
    public void suppressesRepeatedDetectionsWithinTheInterval() {
        BrowserPasskeyHintDebounce debounce = new BrowserPasskeyHintDebounce(10_000L);

        Assert.assertTrue(debounce.shouldShow(0L));
        Assert.assertFalse(debounce.shouldShow(1_000L));
        Assert.assertFalse(debounce.shouldShow(9_999L));
    }

    @Test
    public void showsAgainAfterTheIntervalElapses() {
        BrowserPasskeyHintDebounce debounce = new BrowserPasskeyHintDebounce(10_000L);

        Assert.assertTrue(debounce.shouldShow(0L));
        Assert.assertFalse(debounce.shouldShow(5_000L));
        Assert.assertTrue(debounce.shouldShow(10_000L));
        Assert.assertFalse(debounce.shouldShow(15_000L));
    }

    @Test
    public void defaultIntervalIsTenSeconds() {
        Assert.assertEquals(10_000L, BrowserPasskeyHintDebounce.DEFAULT_MIN_INTERVAL_MS);
    }
}
