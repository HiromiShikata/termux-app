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
        Map<Integer, String> markedSessionAgeLabels = new LinkedHashMap<>();
        markedSessionAgeLabels.put(1, "30s ago");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, NO_TITLES, markedSessionAgeLabels, NO_DISABLED), -1);
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
            rows, sessionRows(names, NO_TITLES, Collections.emptyMap(), NO_DISABLED), -1);
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);

        Assert.assertFalse(structureText.contains("ago"));
    }

    private static Map<Integer, SessionRow> sessionRows(List<String> names, List<String> titles,
                                                        Map<Integer, String> markedSessionAgeLabels,
                                                        Set<Integer> disabledSessionIndexes) {
        return sessionRows(names, titles, markedSessionAgeLabels, disabledSessionIndexes, -1);
    }

    private static Map<Integer, SessionRow> sessionRows(List<String> names, List<String> titles,
                                                        Map<Integer, String> markedSessionAgeLabels,
                                                        Set<Integer> disabledSessionIndexes,
                                                        int currentSessionIndex) {
        return SessionRow.project(names, titles, Collections.emptyList(), Collections.emptyList(),
            markedSessionAgeLabels, disabledSessionIndexes, currentSessionIndex);
    }

    @Test
    public void pickerHidesTheBellSlotForTheSeenCurrentSessionButShowsItForAnUnseenBackgroundSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("current", 1_000L);
        store.recordSeen("current", 5_000L);
        store.recordBell("background", 1_000L);

        long nowMillis = 31_000L;
        List<String> names = Arrays.asList("current", "background");
        Map<Integer, String> markedSessionAgeLabels = new LinkedHashMap<>();
        for (int sessionIndex = 0; sessionIndex < names.size(); sessionIndex++) {
            SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                store, names.get(sessionIndex), nowMillis);
            if (indicator.isVisible()) {
                markedSessionAgeLabels.put(sessionIndex, indicator.getLabel());
            }
        }

        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(
            rows, sessionRows(names, NO_TITLES, markedSessionAgeLabels, NO_DISABLED, 0), -1);

        Assert.assertFalse(SessionSwitchPickerController.isBellMarkSlotVisible(lines.get(0).isMarked()));
        Assert.assertTrue(SessionSwitchPickerController.isBellMarkSlotVisible(lines.get(1).isMarked()));

        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);
        Assert.assertTrue(structureText.contains("30s ago"));
    }

    @Test
    public void bottomSheetLabelAndPickerLabelAreByteIdenticalForTheSameBellTimestamp() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background", 1_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 1_000L + 45_000L);

        String bottomSheetLabel = "  " + indicator.getLabel();
        String pickerLabel = SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel());

        Assert.assertEquals("45s ago", indicator.getLabel());
        Assert.assertEquals(bottomSheetLabel, pickerLabel);
    }

    @Test
    public void aSessionWithoutABellProducesNoIndicatorAndNoLabel() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background", 5_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
        Assert.assertEquals("", SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel()));
    }
}
