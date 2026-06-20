package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionActiveBellSuppressionTest {

    @Test
    public void activeSessionShowsNoBellImmediatelyAfterABellEventBeforeAnySeenTick() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active-handle", 5_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active-handle", "active-handle", 5_050L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void backgroundUnseenSessionStillShowsTheBellWhenADifferentSessionIsActive() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", "active-handle", 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void backgroundUnseenSessionStillShowsTheBellWhenNoSessionIsActive() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", null, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void bothRenderersAgreeThatTheActiveSessionShowsNoBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active-handle", 5_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active-handle", "active-handle", 5_050L);

        int bottomSheetDrawable =
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.isVisible());
        int placeholderDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(false);
        boolean pickerShowsBell = SessionSwitchPickerController.isBellMarkSlotVisible(indicator.isVisible());

        Assert.assertEquals(placeholderDrawable, bottomSheetDrawable);
        Assert.assertFalse(pickerShowsBell);
    }

    @Test
    public void pickerStructureOmitsTheBellForTheActiveSessionEvenWithARecordedBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active-handle", 5_000L);
        store.recordBell("background-handle", 1_000L);

        long nowMillis = 5_050L;
        String activeSessionHandle = "active-handle";
        Map<Integer, String> markedSessionAgeLabels = new LinkedHashMap<>();
        markIfVisible(markedSessionAgeLabels, 0, TermuxSessionsListViewController.newActivityIndicator(
            store, "active-handle", activeSessionHandle, nowMillis));
        markIfVisible(markedSessionAgeLabels, 1, TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", activeSessionHandle, nowMillis));

        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0), SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("active", "background");
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, names, Collections.emptyList(), markedSessionAgeLabels, Collections.emptySet(), 0);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertTrue(lines.get(1).isMarked());
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);
        Assert.assertTrue(structureText.contains("4s ago"));
    }

    @Test
    public void leavingASessionStillRevealsABellThatArrivesAfterItStopsBeingActive() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("handle", 6_000L);
        store.recordBell("handle", 9_000L);

        SessionNewActivityIndicator whileActive = TermuxSessionsListViewController.newActivityIndicator(
            store, "handle", "handle", 9_500L);
        SessionNewActivityIndicator afterLeaving = TermuxSessionsListViewController.newActivityIndicator(
            store, "handle", "other-handle", 9_500L);

        Assert.assertFalse(whileActive.isVisible());
        Assert.assertTrue(afterLeaving.isVisible());
    }

    private static void markIfVisible(Map<Integer, String> markedSessionAgeLabels,
                                      int sessionIndex, SessionNewActivityIndicator indicator) {
        if (indicator.isVisible()) {
            markedSessionAgeLabels.put(sessionIndex, indicator.getLabel());
        }
    }
}
