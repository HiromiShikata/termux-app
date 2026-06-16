package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProjectActionTokenParserTest {

    @Test
    public void parsesProjectActionTokenIntoNormalizedProjectAndAction() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse("xmile:overviewUrl");

        Assert.assertEquals(
            Collections.singletonList(new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL)),
            tokens);
    }

    @Test
    public void parsesAllThreeSupportedActions() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse(
            "xmile:overviewUrl,umino:tdpmConsoleUrl,secretary:newIssueUrl");

        Assert.assertEquals(Arrays.asList(
            new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL),
            new ProjectActionToken("umino", ProjectAction.TDPM_CONSOLE_URL),
            new ProjectActionToken("secretary", ProjectAction.NEW_ISSUE_URL)),
            tokens);
    }

    @Test
    public void matchingIsCaseInsensitiveForProjectAndAction() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse("XMILE:OVERVIEWURL");

        Assert.assertEquals(
            Collections.singletonList(new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL)),
            tokens);
    }

    @Test
    public void trimsWhitespaceAroundProjectAndAction() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse("  xmile : overviewUrl  ");

        Assert.assertEquals(
            Collections.singletonList(new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL)),
            tokens);
    }

    @Test
    public void ignoresTokenWithUnknownAction() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse("xmile:unknownAction");

        Assert.assertEquals(Collections.emptyList(), tokens);
    }

    @Test
    public void ignoresBareProjectNameWithoutActionSegment() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse("xmile");

        Assert.assertEquals(Collections.emptyList(), tokens);
    }

    @Test
    public void ignoresTokenWithEmptyProjectNameSegment() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse(":overviewUrl");

        Assert.assertEquals(Collections.emptyList(), tokens);
    }

    @Test
    public void parsesValidActionTokenWhileIgnoringBareAndUnknownTokens() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse(
            "umino,xmile:overviewUrl,secretary:bogus");

        Assert.assertEquals(
            Collections.singletonList(new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL)),
            tokens);
    }

    @Test
    public void removesDuplicateTokensPreservingFirstOccurrenceOrder() {
        List<ProjectActionToken> tokens = ProjectActionTokenParser.parse(
            "xmile:overviewUrl,XMILE:OverviewUrl");

        Assert.assertEquals(
            Collections.singletonList(new ProjectActionToken("xmile", ProjectAction.OVERVIEW_URL)),
            tokens);
    }

    @Test
    public void returnsEmptyListForNullInput() {
        Assert.assertEquals(Collections.emptyList(), ProjectActionTokenParser.parse(null));
    }

    @Test
    public void returnsEmptyListForBlankInput() {
        Assert.assertEquals(Collections.emptyList(), ProjectActionTokenParser.parse("   "));
    }
}
