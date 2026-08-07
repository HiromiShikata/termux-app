package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class BackgroundedAppStillHasADisplayedSessionSetTest {

    private final DisplayedSessionSelector selector = new DisplayedSessionSelector();

    @Test
    public void theSetSurvivesTheActivityNotBeingVisible() {
        Set<String> displayed = selector.selectDisplayedSessionNamesRegardlessOfActivityVisibility(
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/second"),
            true, Collections.emptySet(), null);

        Assert.assertEquals("the background cycle must still have every displayed session to scan while "
                + "the app is not visible; an empty set makes the cycle return at its guard and detect "
                + "nothing",
            new LinkedHashSet<>(Arrays.asList("https://example.test/current", "https://example.test/second")),
            displayed);
    }

    @Test
    public void hiddenSessionsStayExcluded() {
        Set<String> displayed = selector.selectDisplayedSessionNamesRegardlessOfActivityVisibility(
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/hidden"),
            true, Collections.singleton("https://example.test/hidden"), null);

        Assert.assertFalse("a session the owner hid must stay out of the background cycle",
            displayed.contains("https://example.test/hidden"));
        Assert.assertTrue("the current session must stay in the background cycle",
            displayed.contains("https://example.test/current"));
    }

    @Test
    public void collapsedProjectSessionsStayExcluded() {
        Set<String> displayed = selector.selectDisplayedSessionNamesRegardlessOfActivityVisibility(
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/collapsed"),
            true, Collections.emptySet(),
            Collections.singleton("https://example.test/current"));

        Assert.assertFalse("a session under a collapsed project must stay out of the background cycle",
            displayed.contains("https://example.test/collapsed"));
        Assert.assertTrue("the current session must stay in the background cycle even when the project "
                + "it belongs to is collapsed",
            displayed.contains("https://example.test/current"));
    }

    @Test
    public void theVisibilityGatedSelectionStillReturnsNothingWhileTheActivityIsNotVisible() {
        Set<String> displayed = selector.selectDisplayedSessionNames(false,
            "https://example.test/current",
            Collections.singletonList("https://example.test/current"),
            true, Collections.emptySet());

        Assert.assertTrue("callers that render on-screen rows rely on the gated selection returning "
                + "nothing while the activity is not visible",
            displayed.isEmpty());
    }

    @Test
    public void theVisibilityGatedSelectionStillReturnsTheSameSetWhileTheActivityIsVisible() {
        Set<String> gated = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/second"),
            true, Collections.emptySet());
        Set<String> ungated = selector.selectDisplayedSessionNamesRegardlessOfActivityVisibility(
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/second"),
            true, Collections.emptySet(), null);

        Assert.assertEquals("while the activity is visible the two selections must agree, otherwise the "
                + "background cycle and the on-screen rows would disagree about what is displayed",
            gated, ungated);
    }
}
