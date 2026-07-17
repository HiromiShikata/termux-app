package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionHierarchyBuilderProjectLabelForSessionNameTest {

    private static List<SessionHierarchyRow> sampleRows() {
        return Arrays.asList(
            SessionHierarchyRow.projectHeader("umino"),
            SessionHierarchyRow.storyHeader("umino story"),
            SessionHierarchyRow.session(0, "uminopm"),
            SessionHierarchyRow.session(1, "https://example.com/umino/issue-1"),
            SessionHierarchyRow.projectHeader("xmile"),
            SessionHierarchyRow.session(2, "xmilepm"));
    }

    @Test
    public void returnsProjectLabelForManagerSession() {
        Assert.assertEquals("umino",
            SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(), "uminopm"));
    }

    @Test
    public void returnsProjectLabelForNonManagerSession() {
        Assert.assertEquals("umino",
            SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(),
                "https://example.com/umino/issue-1"));
    }

    @Test
    public void returnsProjectLabelForSecondProjectSession() {
        Assert.assertEquals("xmile",
            SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(), "xmilepm"));
    }

    @Test
    public void returnsNullWhenSessionIsNotPresent() {
        Assert.assertNull(
            SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(), "unknownpm"));
    }

    @Test
    public void returnsNullForNullOrEmptySessionName() {
        Assert.assertNull(SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(), null));
        Assert.assertNull(SessionHierarchyBuilder.projectLabelForSessionName(sampleRows(), ""));
    }

    @Test
    public void returnsNullForEmptyRows() {
        Assert.assertNull(
            SessionHierarchyBuilder.projectLabelForSessionName(Collections.emptyList(), "uminopm"));
    }
}
