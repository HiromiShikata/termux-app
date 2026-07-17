package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ShortcutNavigationProjectExpanderTest {

    private static List<SessionHierarchyRow> sampleRows() {
        return Arrays.asList(
            SessionHierarchyRow.projectHeader("umino"),
            SessionHierarchyRow.storyHeader("umino story"),
            SessionHierarchyRow.session(0, "uminopm"),
            SessionHierarchyRow.session(1, "https://example.com/umino/issue-1"),
            SessionHierarchyRow.projectHeader("xmile"),
            SessionHierarchyRow.storyHeader("xmile story"),
            SessionHierarchyRow.session(2, "xmilepm"));
    }

    @Test
    public void expandsCollapsedProjectWhenNavigatingToSessionInsideIt() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Arrays.asList("umino", "xmile"));

        boolean expanded = ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, "uminopm");

        Assert.assertTrue(expanded);
        Assert.assertFalse(collapsedProjectKeys.contains("umino"));
        Assert.assertTrue(collapsedProjectKeys.contains("xmile"));
    }

    @Test
    public void expandsCollapsedProjectForNonManagerSessionInsideIt() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Arrays.asList("umino"));

        boolean expanded = ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, "https://example.com/umino/issue-1");

        Assert.assertTrue(expanded);
        Assert.assertTrue(collapsedProjectKeys.isEmpty());
    }

    @Test
    public void leavesAlreadyExpandedProjectUnchanged() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Arrays.asList("xmile"));

        boolean expanded = ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, "uminopm");

        Assert.assertFalse(expanded);
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("xmile")), collapsedProjectKeys);
    }

    @Test
    public void returnsFalseWhenSessionIsNotFoundInAnyProject() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Arrays.asList("umino", "xmile"));

        boolean expanded = ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, "unknownpm");

        Assert.assertFalse(expanded);
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("umino", "xmile")), collapsedProjectKeys);
    }

    @Test
    public void returnsFalseForNullOrEmptySessionName() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Arrays.asList("umino"));

        Assert.assertFalse(ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, null));
        Assert.assertFalse(ShortcutNavigationProjectExpander.expandCollapsedProjectForSession(
            sampleRows(), collapsedProjectKeys, ""));
        Assert.assertTrue(collapsedProjectKeys.contains("umino"));
    }
}
