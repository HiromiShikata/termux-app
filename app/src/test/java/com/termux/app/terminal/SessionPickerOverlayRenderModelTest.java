package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionPickerOverlayRenderModelTest {

    @Test
    public void buildsProjectStoryAndSessionLinesFromHierarchy() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("DEMOPROJECT"),
            SessionHierarchyRow.storyHeader("DemoStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, 1);

        Assert.assertEquals(4, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.PROJECT, "DEMOPROJECT", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.STORY, "DemoStory", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(3), SessionPickerOverlayLine.Kind.SESSION, "beta", true);
    }

    @Test
    public void highlightsOnlyTheSessionMatchingTheHighlightedIndex() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(3),
            SessionHierarchyRow.session(5));
        List<String> names = Arrays.asList("s0", "s1", "s2", "s3", "s4", "s5");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, 3);

        Assert.assertFalse(lines.get(0).isHighlighted());
        Assert.assertTrue(lines.get(1).isHighlighted());
        Assert.assertFalse(lines.get(2).isHighlighted());
    }

    @Test
    public void fallsBackToGenericLabelWhenDisplayNameIsMissingOrEmpty() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("", null);

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, -1);

        Assert.assertEquals("session 0", lines.get(0).getText());
        Assert.assertEquals("session 1", lines.get(1).getText());
    }

    @Test
    public void fallsBackToGenericLabelWhenSessionIndexIsOutOfRange() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(7));
        List<String> names = Arrays.asList("only");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, -1);

        Assert.assertEquals("session 7", lines.get(0).getText());
    }

    private void assertLine(SessionPickerOverlayLine line, SessionPickerOverlayLine.Kind kind,
                            String text, boolean highlighted) {
        Assert.assertEquals(kind, line.getKind());
        Assert.assertEquals(text, line.getText());
        Assert.assertEquals(highlighted, line.isHighlighted());
    }
}
