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
    public void activeSessionShowsNoDotOnceTheUserHasRepliedPastTheSignal() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("active", 5_000L, "needs approval");
        store.recordUserInput("active", 5_050L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active", 5_050L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void backgroundUnansweredSessionStillShowsTheDotWhileTheRepliedSessionIsCleared() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("background", 1_000L, "needs approval");
        store.recordExplicitCall("active", 5_000L, "needs approval");
        store.recordUserInput("active", 31_000L);

        SessionNewActivityIndicator background = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 31_000L);

        Assert.assertTrue(background.isVisible());
        Assert.assertEquals("30s ago", background.getLabel());
    }

    @Test
    public void bothRenderersAgreeThatTheRepliedSessionShowsNoDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("active", 5_000L, "needs approval");
        store.recordUserInput("active", 5_050L);

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "active", 5_050L);

        int bottomSheetDrawable =
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.getTier());
        int placeholderDrawable = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(
            SessionNewActivityTier.NONE);
        boolean pickerShowsDot = SessionSwitchPickerController.isBellMarkSlotVisible(indicator.isVisible());

        Assert.assertEquals(placeholderDrawable, bottomSheetDrawable);
        Assert.assertFalse(pickerShowsDot);
    }

    @Test
    public void pickerOmitsTheDotForTheRepliedCurrentSessionAndKeepsItForTheBackgroundSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("active", 5_000L, "needs approval");
        store.recordUserInput("active", 5_050L);
        store.recordExplicitCall("background", 1_000L, "needs approval");

        long nowMillis = 5_050L;
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        Map<Integer, String> ageLabelsByIndex = new LinkedHashMap<>();
        markIfVisible(tiersByIndex, ageLabelsByIndex, 0,
            TermuxSessionsListViewController.newActivityIndicator(store, "active", nowMillis));
        markIfVisible(tiersByIndex, ageLabelsByIndex, 1,
            TermuxSessionsListViewController.newActivityIndicator(store, "background", nowMillis));

        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0), SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("active", "background");
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, Collections.emptyList(), tiersByIndex, ageLabelsByIndex,
                Collections.emptySet()), 0);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertTrue(lines.get(1).isMarked());
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);
        Assert.assertTrue(structureText.contains("4s ago"));
    }

    @Test
    public void currentSessionWithoutASeenTickStillShowsItsDotThroughTheSharedLogic() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("current", 1_000L, "needs approval");

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "current", 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void aSignalArrivingAfterTheLastSeenTickReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("session", 6_000L);
        store.recordExplicitCall("session", 9_000L, "needs approval");

        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "session", 9_500L);

        Assert.assertTrue(indicator.isVisible());
    }

    private static Map<Integer, SessionRow> sessionRows(List<String> names, List<String> titles,
                                                        Map<Integer, SessionNewActivityTier> tiersByIndex,
                                                        Map<Integer, String> ageLabelsByIndex,
                                                        Set<Integer> disabledSessionIndexes) {
        return SessionRow.project(names, titles, Collections.emptyList(), Collections.emptyList(),
            tiersByIndex, ageLabelsByIndex, disabledSessionIndexes, -1);
    }

    private static void markIfVisible(Map<Integer, SessionNewActivityTier> tiersByIndex,
                                      Map<Integer, String> ageLabelsByIndex,
                                      int sessionIndex, SessionNewActivityIndicator indicator) {
        if (indicator.isVisible()) {
            tiersByIndex.put(sessionIndex, indicator.getTier());
            ageLabelsByIndex.put(sessionIndex, indicator.getLabel());
        }
    }
}
