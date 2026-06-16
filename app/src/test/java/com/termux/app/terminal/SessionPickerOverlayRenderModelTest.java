package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionPickerOverlayRenderModelTest {

    private static final List<String> NO_TITLES = Collections.emptyList();

    @Test
    public void buildsProjectStoryAndSessionLinesFromHierarchy() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("DEMOPROJECT"),
            SessionHierarchyRow.storyHeader("DemoStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, 1);

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

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, 3);

        Assert.assertFalse(lines.get(0).isHighlighted());
        Assert.assertTrue(lines.get(1).isHighlighted());
        Assert.assertFalse(lines.get(2).isHighlighted());
    }

    @Test
    public void fallsBackToGenericLabelWhenRawNameIsMissingOrEmpty() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("", null);

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, -1);

        Assert.assertEquals("session 0", lines.get(0).getText());
        Assert.assertEquals("session 1", lines.get(1).getText());
    }

    @Test
    public void fallsBackToGenericLabelWhenSessionIndexIsOutOfRange() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(7));
        List<String> names = Arrays.asList("only");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, -1);

        Assert.assertEquals("session 7", lines.get(0).getText());
    }

    @Test
    public void githubShortensRawNameForThePrimaryText() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://github.com/HiromiShikata/termux-app/issues/440");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, -1);

        Assert.assertEquals("HiromiShikata/termux-app/issues/440", lines.get(0).getText());
    }

    @Test
    public void leavesNonGithubNameUnchangedAsPrimaryText() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://example.com/path");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, -1);

        Assert.assertEquals("https://example.com/path", lines.get(0).getText());
    }

    @Test
    public void usesDefinitionTitleAsSecondaryTextWhenPresent() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://github.com/HiromiShikata/termux-app/issues/440");
        List<String> titles = Arrays.asList("Redesign overlay");

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, titles, -1);

        Assert.assertEquals("HiromiShikata/termux-app/issues/440", lines.get(0).getText());
        Assert.assertEquals("Redesign overlay", lines.get(0).getSecondaryText());
    }

    @Test
    public void leavesSecondaryTextEmptyWhenTitleIsMissingOrEmpty() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");
        List<String> titles = Arrays.asList("", null);

        List<SessionPickerOverlayLine> lines = SessionPickerOverlayRenderModel.build(rows, names, titles, -1);

        Assert.assertEquals("", lines.get(0).getSecondaryText());
        Assert.assertEquals("", lines.get(1).getSecondaryText());
    }

    private void assertLine(SessionPickerOverlayLine line, SessionPickerOverlayLine.Kind kind,
                            String text, boolean highlighted) {
        Assert.assertEquals(kind, line.getKind());
        Assert.assertEquals(text, line.getText());
        Assert.assertEquals(highlighted, line.isHighlighted());
    }
}
