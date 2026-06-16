package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TermuxSessionsListViewControllerCollapsedProjectLabelsTest {

    @Test
    public void collapsesEveryProjectNotInTheAllowlist() {
        List<String> projectLabels = Arrays.asList("umino", "xmile", "secretary", "N/A");

        Set<String> collapsed = TermuxSessionsListViewController.collapsedProjectLabels(
            projectLabels, Arrays.asList("umino", "xmile"));

        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("secretary", "N/A")), collapsed);
    }

    @Test
    public void naTokenMatchesNaGroupHeaderLabel() {
        List<String> projectLabels = Arrays.asList("umino", "N/A");

        Set<String> collapsed = TermuxSessionsListViewController.collapsedProjectLabels(
            projectLabels, Collections.singletonList("n/a"));

        Assert.assertEquals(Collections.singleton("umino"), collapsed);
    }

    @Test
    public void matchingIsCaseInsensitiveAndTrimmed() {
        List<String> projectLabels = Arrays.asList("Umino", "XMile", "Secretary");

        Set<String> collapsed = TermuxSessionsListViewController.collapsedProjectLabels(
            projectLabels, Arrays.asList("  UMINO  ", "xmile"));

        Assert.assertEquals(Collections.singleton("Secretary"), collapsed);
    }

    @Test
    public void collapsesAllProjectsWhenAllowlistMatchesNone() {
        List<String> projectLabels = Arrays.asList("umino", "xmile");

        Set<String> collapsed = TermuxSessionsListViewController.collapsedProjectLabels(
            projectLabels, Collections.singletonList("nonexistent"));

        Assert.assertEquals(new LinkedHashSet<>(projectLabels), collapsed);
    }

    @Test
    public void collapsesNoProjectsWhenEveryProjectIsAllowlisted() {
        List<String> projectLabels = Arrays.asList("umino", "xmile", "N/A");

        Set<String> collapsed = TermuxSessionsListViewController.collapsedProjectLabels(
            projectLabels, Arrays.asList("umino", "xmile", "n/a"));

        Assert.assertTrue(collapsed.isEmpty());
    }
}
