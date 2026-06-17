package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionOutputActivityMarkerTest {

    @Test
    public void backgroundSessionOutputMarksTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        boolean marked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, "current-handle", "background-handle");

        Assert.assertTrue(marked);
        Assert.assertTrue(store.hasOutputActivity("background-handle"));
    }

    @Test
    public void currentSessionOutputDoesNotMarkTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        boolean marked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, "current-handle", "current-handle");

        Assert.assertFalse(marked);
        Assert.assertFalse(store.hasOutputActivity("current-handle"));
    }

    @Test
    public void backgroundSessionOutputWithNoCurrentSessionMarksTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        boolean marked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, null, "background-handle");

        Assert.assertTrue(marked);
        Assert.assertTrue(store.hasOutputActivity("background-handle"));
    }

    @Test
    public void outputFromSessionWithNullHandleDoesNotMarkTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        boolean marked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, "current-handle", null);

        Assert.assertFalse(marked);
    }

    @Test
    public void switchingToFlaggedSessionClearsTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        store.markOutputActivity("background-handle");

        SessionOutputActivityMarker.clearOutputActivityForCurrentSession(store, "background-handle");

        Assert.assertFalse(store.hasOutputActivity("background-handle"));
    }

    @Test
    public void clearingDoesNotAffectOtherFlaggedSessions() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        store.markOutputActivity("background-handle-one");
        store.markOutputActivity("background-handle-two");

        SessionOutputActivityMarker.clearOutputActivityForCurrentSession(store, "background-handle-one");

        Assert.assertFalse(store.hasOutputActivity("background-handle-one"));
        Assert.assertTrue(store.hasOutputActivity("background-handle-two"));
    }
}
