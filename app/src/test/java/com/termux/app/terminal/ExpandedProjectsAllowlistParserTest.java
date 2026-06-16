package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExpandedProjectsAllowlistParserTest {

    @Test
    public void parsesCommaSeparatedProjectsIntoNormalizedTokens() {
        Assert.assertEquals(Arrays.asList("n/a", "umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse("n/a,umino,xmile"));
    }

    @Test
    public void trimsWhitespaceAroundEachToken() {
        Assert.assertEquals(Arrays.asList("n/a", "umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse(" n/a , umino ,  xmile "));
    }

    @Test
    public void lowercasesTokensSoMatchingIsCaseInsensitive() {
        Assert.assertEquals(Arrays.asList("n/a", "umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse("N/A,Umino,XMILE"));
    }

    @Test
    public void dropsEmptyTokensFromRepeatedOrTrailingSeparators() {
        Assert.assertEquals(Arrays.asList("umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse(",umino,,xmile,"));
    }

    @Test
    public void removesDuplicateTokensPreservingFirstOccurrenceOrder() {
        Assert.assertEquals(Arrays.asList("umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse("umino,xmile,Umino,XMILE"));
    }

    @Test
    public void returnsEmptyListForNullInput() {
        Assert.assertEquals(Collections.emptyList(), ExpandedProjectsAllowlistParser.parse(null));
    }

    @Test
    public void returnsEmptyListForBlankInput() {
        Assert.assertEquals(Collections.emptyList(), ExpandedProjectsAllowlistParser.parse("   "));
    }

    @Test
    public void parsesSingleProjectToken() {
        Assert.assertEquals(Collections.singletonList("umino"),
            ExpandedProjectsAllowlistParser.parse("umino"));
    }

    @Test
    public void normalizeTrimsAndLowercases() {
        Assert.assertEquals("n/a", ExpandedProjectsAllowlistParser.normalize("  N/A  "));
    }

    @Test
    public void stripsActionSuffixSoBareProjectNameRemainsAnAllowlistToken() {
        Assert.assertEquals(Collections.singletonList("xmile"),
            ExpandedProjectsAllowlistParser.parse("xmile:overviewUrl"));
    }

    @Test
    public void keepsBareAndActionTokensCollapsedToProjectNamesWithoutDuplicates() {
        Assert.assertEquals(Arrays.asList("umino", "xmile"),
            ExpandedProjectsAllowlistParser.parse("umino,xmile:overviewUrl,XMILE:tdpmConsoleUrl"));
    }
}
