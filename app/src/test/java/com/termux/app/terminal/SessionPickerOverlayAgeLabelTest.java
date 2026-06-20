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

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, markedSessionAgeLabels, NO_DISABLED, -1);
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
            rows, names, NO_TITLES, Collections.emptyMap(), NO_DISABLED, -1);
        String structureText = SessionSwitchPickerController.pickerStructurePlainText(lines);

        Assert.assertFalse(structureText.contains("ago"));
    }

    @Test
    public void bottomSheetLabelAndPickerLabelAreByteIdenticalForTheSameBellTimestamp() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", 1_000L + 45_000L);

        String bottomSheetLabel = "  " + indicator.getLabel();
        String pickerLabel = SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel());

        Assert.assertEquals("45s ago", indicator.getLabel());
        Assert.assertEquals(bottomSheetLabel, pickerLabel);
    }

    @Test
    public void aSessionWithoutABellProducesNoIndicatorAndNoLabel() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
            store, "background-handle", 5_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
        Assert.assertEquals("", SessionSwitchPickerController.newActivityLabelSlotText(indicator.getLabel()));
    }
}
