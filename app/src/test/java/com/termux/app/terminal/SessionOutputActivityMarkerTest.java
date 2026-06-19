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
    public void outputWithNoEstablishedCurrentSessionDoesNotMarkTheFlag() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();

        boolean marked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, null, "background-handle");

        Assert.assertFalse(marked);
        Assert.assertFalse(store.hasOutputActivity("background-handle"));
    }

    @Test
    public void outputForTheSessionJustSwitchedToDoesNotRemarkAfterClear() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        store.markOutputActivity("just-viewed-handle");

        SessionOutputActivityMarker.clearOutputActivityForCurrentSession(store, "just-viewed-handle");

        boolean remarked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, "just-viewed-handle", "just-viewed-handle");

        Assert.assertFalse(remarked);
        Assert.assertFalse(store.hasOutputActivity("just-viewed-handle"));
    }

    @Test
    public void newOutputInAGenuineBackgroundSessionRemarksAfterViewingAnother() {
        SessionOutputActivityStore store = new SessionOutputActivityStore();
        store.markOutputActivity("background-handle");

        SessionOutputActivityMarker.clearOutputActivityForCurrentSession(store, "background-handle");
        Assert.assertFalse(store.hasOutputActivity("background-handle"));

        boolean remarked = SessionOutputActivityMarker.markBackgroundOutputActivity(
            store, "now-current-handle", "background-handle");

        Assert.assertTrue(remarked);
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
