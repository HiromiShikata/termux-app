package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SessionPickerOverlayRenderModelTest {

    private static final List<String> NO_TITLES = Collections.emptyList();
    private static final Set<Integer> NO_MARKS = Collections.emptySet();
    private static final Set<Integer> NO_DISABLED = Collections.emptySet();

    @Test
    public void groupsStoryDirectlyUnderItsProjectWithoutASpacerBetweenThem() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("DEMOPROJECT"),
            SessionHierarchyRow.storyHeader("DemoStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, 1);

        Assert.assertEquals(4, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.PROJECT, "DEMOPROJECT", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.STORY, "DemoStory", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(3), SessionPickerOverlayLine.Kind.SESSION, "beta", true);
    }

    @Test
    public void insertsSpacerBeforeEachProjectHeaderThatFollowsAnotherRow() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("FIRSTPROJECT"),
            SessionHierarchyRow.storyHeader("FirstStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.projectHeader("SECONDPROJECT"),
            SessionHierarchyRow.storyHeader("SecondStory"),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals(7, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.PROJECT, "FIRSTPROJECT", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.STORY, "FirstStory", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(3), SessionPickerOverlayLine.Kind.SPACER, "", false);
        assertLine(lines.get(4), SessionPickerOverlayLine.Kind.PROJECT, "SECONDPROJECT", false);
        assertLine(lines.get(5), SessionPickerOverlayLine.Kind.STORY, "SecondStory", false);
        assertLine(lines.get(6), SessionPickerOverlayLine.Kind.SESSION, "beta", false);
    }

    @Test
    public void insertsSpacerBetweenSiblingStoriesInAProjectlessList() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.storyHeader("FirstStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.storyHeader("SecondStory"),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals(5, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.STORY, "FirstStory", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SPACER, "", false);
        assertLine(lines.get(3), SessionPickerOverlayLine.Kind.STORY, "SecondStory", false);
        assertLine(lines.get(4), SessionPickerOverlayLine.Kind.SESSION, "beta", false);
    }

    @Test
    public void doesNotInsertSpacerBeforeTheVeryFirstStoryHeader() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.storyHeader("OnlyStory"),
            SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("alpha");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals(2, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.STORY, "OnlyStory", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
    }

    @Test
    public void highlightsOnlyTheSessionMatchingTheHighlightedIndex() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(3),
            SessionHierarchyRow.session(5));
        List<String> names = Arrays.asList("s0", "s1", "s2", "s3", "s4", "s5");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, 3);

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

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals("session 0", lines.get(0).getText());
        Assert.assertEquals("session 1", lines.get(1).getText());
    }

    @Test
    public void fallsBackToGenericLabelWhenSessionIndexIsOutOfRange() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(7));
        List<String> names = Arrays.asList("only");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals("session 7", lines.get(0).getText());
    }

    @Test
    public void githubShortensRawNameForThePrimaryText() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://github.com/HiromiShikata/termux-app/issues/440");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals("HiromiShikata/termux-app/issues/440", lines.get(0).getText());
    }

    @Test
    public void leavesNonGithubNameUnchangedAsPrimaryText() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://example.com/path");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals("https://example.com/path", lines.get(0).getText());
    }

    @Test
    public void usesDefinitionTitleAsSecondaryTextWhenPresent() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("https://github.com/HiromiShikata/termux-app/issues/440");
        List<String> titles = Arrays.asList("Redesign overlay");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, titles, NO_MARKS, NO_DISABLED, -1);

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

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, titles, NO_MARKS, NO_DISABLED, -1);

        Assert.assertEquals("", lines.get(0).getSecondaryText());
        Assert.assertEquals("", lines.get(1).getSecondaryText());
    }

    @Test
    public void marksOnlySessionsWhoseIndexIsInTheMarkedSet() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1),
            SessionHierarchyRow.session(2));
        List<String> names = Arrays.asList("alpha", "beta", "gamma");
        Set<Integer> markedSessionIndexes = new HashSet<>(Arrays.asList(0, 2));

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, markedSessionIndexes, NO_DISABLED, -1);

        Assert.assertTrue(lines.get(0).isMarked());
        Assert.assertFalse(lines.get(1).isMarked());
        Assert.assertTrue(lines.get(2).isMarked());
    }

    @Test
    public void leavesSessionsUnmarkedWhenMarkedSetIsEmpty() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, NO_DISABLED, -1);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertFalse(lines.get(1).isMarked());
    }

    @Test
    public void excludesDisabledSessionFromRenderedLines() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.storyHeader("DemoStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.session(1),
            SessionHierarchyRow.session(2));
        List<String> names = Arrays.asList("alpha", "beta", "gamma");
        Set<Integer> disabledSessionIndexes = new HashSet<>(Collections.singletonList(1));

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, disabledSessionIndexes, -1);

        Assert.assertEquals(3, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.STORY, "DemoStory", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SESSION, "gamma", false);
    }

    @Test
    public void dropsStoryHeaderWhoseOnlySessionIsDisabledWithoutDanglingSpacer() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.storyHeader("FirstStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.storyHeader("DisabledOnlyStory"),
            SessionHierarchyRow.session(1),
            SessionHierarchyRow.storyHeader("LastStory"),
            SessionHierarchyRow.session(2));
        List<String> names = Arrays.asList("alpha", "beta", "gamma");
        Set<Integer> disabledSessionIndexes = new HashSet<>(Collections.singletonList(1));

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, disabledSessionIndexes, -1);

        Assert.assertEquals(5, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.STORY, "FirstStory", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.SESSION, "alpha", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SPACER, "", false);
        assertLine(lines.get(3), SessionPickerOverlayLine.Kind.STORY, "LastStory", false);
        assertLine(lines.get(4), SessionPickerOverlayLine.Kind.SESSION, "gamma", false);
    }

    @Test
    public void dropsLeadingStoryHeaderWhoseOnlySessionIsDisabledAndKeepsRemainingStoryUnderProject() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("DEMOPROJECT"),
            SessionHierarchyRow.storyHeader("DisabledOnlyStory"),
            SessionHierarchyRow.session(0),
            SessionHierarchyRow.storyHeader("RemainingStory"),
            SessionHierarchyRow.session(1));
        List<String> names = Arrays.asList("alpha", "beta");
        Set<Integer> disabledSessionIndexes = new HashSet<>(Collections.singletonList(0));

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, NO_TITLES, NO_MARKS, disabledSessionIndexes, -1);

        Assert.assertEquals(3, lines.size());
        assertLine(lines.get(0), SessionPickerOverlayLine.Kind.PROJECT, "DEMOPROJECT", false);
        assertLine(lines.get(1), SessionPickerOverlayLine.Kind.STORY, "RemainingStory", false);
        assertLine(lines.get(2), SessionPickerOverlayLine.Kind.SESSION, "beta", false);
    }

    @Test
    public void keepsShortSecondaryTextUntruncated() {
        Assert.assertEquals("Redesign overlay",
            SessionPickerOverlayRenderModel.truncateSecondaryToSingleLine("Redesign overlay"));
    }

    @Test
    public void truncatesLongSecondaryTextWithEllipsis() {
        String longDescription =
            "This is a very long session description that should not wrap onto a second overlay line";

        String truncated = SessionPickerOverlayRenderModel.truncateSecondaryToSingleLine(longDescription);

        Assert.assertTrue(truncated.length() <= SessionPickerOverlayRenderModel.SECONDARY_MAX_CHARACTERS);
        Assert.assertTrue(truncated.length() < longDescription.length());
        Assert.assertTrue(truncated.endsWith("…"));
        Assert.assertTrue(longDescription.startsWith(truncated.substring(0, truncated.length() - 1)));
    }

    @Test
    public void collapsesNewlinesInSecondaryTextToSingleLine() {
        String multilineDescription = "first line\nsecond line";

        String singleLine = SessionPickerOverlayRenderModel.truncateSecondaryToSingleLine(multilineDescription);

        Assert.assertFalse(singleLine.contains("\n"));
        Assert.assertEquals("first line second line", singleLine);
    }

    @Test
    public void truncatesLongSecondaryTitleThroughBuild() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        List<String> names = Arrays.asList("alpha");
        List<String> titles = Arrays.asList(
            "This is a very long session description that should never wrap to a second line");

        List<SessionPickerOverlayLine> lines =
            SessionPickerOverlayRenderModel.build(rows, names, titles, NO_MARKS, NO_DISABLED, -1);

        String secondaryText = lines.get(0).getSecondaryText();
        Assert.assertEquals(SessionPickerOverlayRenderModel.SECONDARY_MAX_CHARACTERS, secondaryText.length());
        Assert.assertTrue(secondaryText.endsWith("…"));
    }

    private void assertLine(SessionPickerOverlayLine line, SessionPickerOverlayLine.Kind kind,
                            String text, boolean highlighted) {
        Assert.assertEquals(kind, line.getKind());
        Assert.assertEquals(text, line.getText());
        Assert.assertEquals(highlighted, line.isHighlighted());
    }
}
