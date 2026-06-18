package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionOutputActivityStoreTest {

    @Test
    public void hasNoOutputActivityBeforeMarking() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        Assert.assertFalse(store.hasOutputActivity("handle-one"));
    }

    @Test
    public void marksAndClearsOutputActivityByHandle() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        store.markOutputActivity("handle-one");
        Assert.assertTrue(store.hasOutputActivity("handle-one"));

        store.clearOutputActivity("handle-one");
        Assert.assertFalse(store.hasOutputActivity("handle-one"));
    }

    @Test
    public void tracksOutputActivityForDistinctHandlesIndependently() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        store.markOutputActivity("handle-one");
        store.markOutputActivity("handle-two");

        store.clearOutputActivity("handle-one");

        Assert.assertFalse(store.hasOutputActivity("handle-one"));
        Assert.assertTrue(store.hasOutputActivity("handle-two"));
    }
}
