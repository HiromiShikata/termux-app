package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionPickerOverlayAgeLabelTest {

    private static final List<String> NO_TITLES = Collections.emptyList();
    private static final Set<Integer> NO_DISABLED = Collections.emptySet();

    @Test
    public void renderedPickerStructureTextContainsTheAgeLabelForAMarkedSession() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        tiersByIndex.put(1, SessionNewActivityTier.YELLOW);
        Map<Integer, String> ageLabelsByIndex = new LinkedHashMap<>();
        ageLabelsByIndex.put(1, "30s ago");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, NO_TITLES, tiersByIndex, ageLabelsByIndex, NO_DISABLED, -1), -1);
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);

        Assert.assertTrue(structureText.contains("30s ago"));
    }

    @Test
    public void renderedPickerStructureTextOmitsAnyAgeLabelWhenNoSessionIsMarked() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, NO_TITLES, Collections.emptyMap(), Collections.emptyMap(),
                NO_DISABLED, -1), -1);
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);

        Assert.assertFalse(structureText.contains("ago"));
    }

    private static Map<Integer, SessionRow> sessionRows(List<String> names, List<String> titles,
                                                        Map<Integer, SessionNewActivityTier> tiersByIndex,
                                                        Map<Integer, String> ageLabelsByIndex,
                                                        Set<Integer> disabledSessionIndexes,
                                                        int currentSessionIndex) {
        return SessionRow.project(names, titles, Collections.emptyList(), Collections.emptyList(),
            tiersByIndex, ageLabelsByIndex, disabledSessionIndexes, currentSessionIndex);
    }

    @Test
    public void pickerHidesTheDotForTheSeenCurrentSessionButShowsItForAnUnseenBackgroundSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("current", 1_000L);
        store.recordSeen("current", 5_000L);
        store.recordExplicitCall("background", 1_000L);

        long nowMillis = 31_000L;
        List<String> names = Arrays.asList("current", "background");
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        Map<Integer, String> ageLabelsByIndex = new LinkedHashMap<>();
        for (int sessionIndex = 0; sessionIndex < names.size(); sessionIndex++) {
            SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                store, names.get(sessionIndex), nowMillis);
            if (indicator.isVisible()) {
                tiersByIndex.put(sessionIndex, indicator.getTier());
                ageLabelsByIndex.put(sessionIndex, indicator.getLabel());
            }
        }

        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, NO_TITLES, tiersByIndex, ageLabelsByIndex, NO_DISABLED, 0), -1);

        Assert.assertFalse(SessionSwitchPickerController.isBellMarkSlotVisible(lines.get(0).isMarked()));
        Assert.assertTrue(SessionSwitchPickerController.isBellMarkSlotVisible(lines.get(1).isMarked()));

        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);
        Assert.assertTrue(structureText.contains("30s ago"));
    }

    @Test
    public void bottomSheetLabelAndPickerLabelAreByteIdenticalForTheSameSignalTimestamp() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("background", 1_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 1_000L + 45_000L);

        String bottomSheetLabel = "  " + indicator.getLabel();
        String pickerLabel = SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel());

        Assert.assertEquals("45s ago", indicator.getLabel());
        Assert.assertEquals(bottomSheetLabel, pickerLabel);
    }

    @Test
    public void aSessionWithoutAnySignalProducesNoIndicatorAndNoLabel() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 5_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
        Assert.assertEquals("", SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel()));
    }
}
