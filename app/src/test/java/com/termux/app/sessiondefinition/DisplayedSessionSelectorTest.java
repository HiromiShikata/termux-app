package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class DisplayedSessionSelectorTest {

    private final DisplayedSessionSelector selector = new DisplayedSessionSelector();

    @Test
    public void includesNonForegroundDisplayedSessionWhenSessionListClosed() {
        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/background-displayed"),
            true, Collections.emptySet());

        Assert.assertTrue(
            "a displayed non-foreground session must be selected for the near-live call scan even when "
                + "the session-list sheet is closed",
            displayed.contains("https://example.test/background-displayed"));
    }

    @Test
    public void includesAllLiveSessionsWhenNoneAreHidden() {
        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current",
                "https://example.test/second", "https://example.test/third"),
            true, Collections.emptySet());

        Assert.assertEquals(
            new LinkedHashSet<>(Arrays.asList("https://example.test/current",
                "https://example.test/second", "https://example.test/third")),
            displayed);
    }

    @Test
    public void excludesHiddenSessionsWhenHideHiddenIsOn() {
        Set<String> hidden = new LinkedHashSet<>(
            Collections.singletonList("https://example.test/hidden"));

        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current",
                "https://example.test/hidden", "https://example.test/shown"),
            true, hidden);

        Assert.assertFalse(displayed.contains("https://example.test/hidden"));
        Assert.assertTrue(displayed.contains("https://example.test/shown"));
        Assert.assertTrue(displayed.contains("https://example.test/current"));
    }

    @Test
    public void includesHiddenSessionsWhenHideHiddenIsOff() {
        Set<String> hidden = new LinkedHashSet<>(
            Collections.singletonList("https://example.test/disabled"));

        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/disabled"),
            false, hidden);

        Assert.assertTrue(
            "when the owner is not hiding hidden sessions they are on-screen and must be scanned",
            displayed.contains("https://example.test/disabled"));
    }

    @Test
    public void keepsForegroundSessionEvenWhenItIsMarkedHidden() {
        Set<String> hidden = new LinkedHashSet<>(
            Collections.singletonList("https://example.test/current"));

        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Collections.singletonList("https://example.test/current"),
            true, hidden);

        Assert.assertTrue(
            "the foreground session is always monitored and must never be excluded",
            displayed.contains("https://example.test/current"));
    }

    @Test
    public void selectsNoSessionsWhenActivityNotVisible() {
        Set<String> displayed = selector.selectDisplayedSessionNames(false,
            "https://example.test/current",
            Arrays.asList("https://example.test/current", "https://example.test/other"),
            true, Collections.emptySet());

        Assert.assertTrue(
            "the fast displayed-session cycle pauses when the app is backgrounded",
            displayed.isEmpty());
    }

    @Test
    public void toleratesNullCurrentSession() {
        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            null, Collections.singletonList("https://example.test/only"),
            true, Collections.emptySet());

        Assert.assertEquals(
            Collections.singleton("https://example.test/only"), displayed);
    }

    @Test
    public void ignoresNullLiveSessionNames() {
        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Arrays.asList("https://example.test/current", null),
            true, Collections.emptySet());

        Assert.assertEquals(
            Collections.singleton("https://example.test/current"), displayed);
    }

    @Test
    public void includesCurrentSessionEvenWhenNotInLiveList() {
        Set<String> displayed = selector.selectDisplayedSessionNames(true,
            "https://example.test/current",
            Collections.singletonList("https://example.test/other"),
            true, Collections.emptySet());

        Assert.assertTrue(displayed.contains("https://example.test/current"));
        Assert.assertTrue(displayed.contains("https://example.test/other"));
    }
}
