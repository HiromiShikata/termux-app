package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionOutputActivityRefreshDebouncerTest {

    @Test
    public void firstOutputAlwaysTriggersRefresh() {
        SessionOutputActivityRefreshDebouncer debouncer = new SessionOutputActivityRefreshDebouncer(500L);
        Assert.assertTrue(debouncer.shouldRefresh(1_000L));
    }

    @Test
    public void continuousOutputWithinIntervalDoesNotTriggerRefresh() {
        SessionOutputActivityRefreshDebouncer debouncer = new SessionOutputActivityRefreshDebouncer(500L);
        debouncer.shouldRefresh(1_000L);

        Assert.assertFalse(debouncer.shouldRefresh(1_100L));
        Assert.assertFalse(debouncer.shouldRefresh(1_400L));
    }

    @Test
    public void outputAfterIntervalTriggersRefreshAgain() {
        SessionOutputActivityRefreshDebouncer debouncer = new SessionOutputActivityRefreshDebouncer(500L);
        debouncer.shouldRefresh(1_000L);
        Assert.assertFalse(debouncer.shouldRefresh(1_200L));

        Assert.assertTrue(debouncer.shouldRefresh(1_500L));
    }

    @Test
    public void refreshIntervalIsMeasuredFromLastRefreshNotLastCall() {
        SessionOutputActivityRefreshDebouncer debouncer = new SessionOutputActivityRefreshDebouncer(500L);
        Assert.assertTrue(debouncer.shouldRefresh(1_000L));
        Assert.assertFalse(debouncer.shouldRefresh(1_400L));
        Assert.assertTrue(debouncer.shouldRefresh(1_500L));
        Assert.assertFalse(debouncer.shouldRefresh(1_900L));
    }
}
