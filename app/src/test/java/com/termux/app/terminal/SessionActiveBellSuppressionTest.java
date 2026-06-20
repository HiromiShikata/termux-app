package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionActiveBellSuppressionTest {

    @Test
    public void activeSessionShowsNoBellOnceTheSeenTickHasAdvancedPastTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active", 5_000L);
        store.recordSeen("active", 5_050L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active", 5_050L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void backgroundUnseenSessionStillShowsTheBellWhileTheActiveSessionIsCleared() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background", 1_000L);
        store.recordBell("active", 5_000L);
        store.recordSeen("active", 31_000L);

        SessionNewActivityIndicator background = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 31_000L);

        Assert.assertTrue(background.isVisible());
        Assert.assertEquals("30s ago", background.getLabel());
    }

    @Test
    public void bothRenderersAgreeThatTheClearedSessionShowsNoBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active", 5_000L);
        store.recordSeen("active", 5_050L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active", 5_050L);

        int bottomSheetDrawable =
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.isVisible());
        int placeholderDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(false);
        boolean pickerShowsBell = SessionSwitchPickerController.isBellMarkSlotVisible(indicator.isVisible());

        Assert.assertEquals(placeholderDrawable, bottomSheetDrawable);
        Assert.assertFalse(pickerShowsBell);
    }

    @Test
    public void pickerOmitsTheBellForTheSeenCurrentSessionAndKeepsItForTheBackgroundSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("active", 5_000L);
        store.recordSeen("active", 5_050L);
        store.recordBell("background", 1_000L);

        long nowMillis = 5_050L;
        Map<Integer, String> markedSessionAgeLabels = new LinkedHashMap<>();
        markIfVisible(markedSessionAgeLabels, 0, TermuxSessionsListViewController.newActivityIndicator(
            store, "active", nowMillis));
        markIfVisible(markedSessionAgeLabels, 1, TermuxSessionsListViewController.newActivityIndicator(
            store, "background", nowMillis));

        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0), SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("active", "background");
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, Collections.emptyList(), markedSessionAgeLabels, Collections.emptySet()), 0);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertTrue(lines.get(1).isMarked());
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);
        Assert.assertTrue(structureText.contains("4s ago"));
    }

    @Test
    public void currentSessionWithoutASeenTickStillShowsItsBellThroughTheSharedLogic() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("current", 1_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "current", 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void aBellArrivingAfterTheLastSeenTickReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("session", 6_000L);
        store.recordBell("session", 9_000L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "session", 9_500L);

        Assert.assertTrue(indicator.isVisible());
    }

    private static Map<Integer, SessionRow> sessionRows(List<String> names, List<String> titles,
                                                        Map<Integer, String> markedSessionAgeLabels,
                                                        Set<Integer> disabledSessionIndexes) {
        return SessionRow.project(names, titles, Collections.emptyList(), Collections.emptyList(),
            markedSessionAgeLabels, disabledSessionIndexes, -1);
    }

    private static void markIfVisible(Map<Integer, String> markedSessionAgeLabels,
                                      int sessionIndex, SessionNewActivityIndicator indicator) {
        if (indicator.isVisible()) {
            markedSessionAgeLabels.put(sessionIndex, indicator.getLabel());
        }
    }
}
