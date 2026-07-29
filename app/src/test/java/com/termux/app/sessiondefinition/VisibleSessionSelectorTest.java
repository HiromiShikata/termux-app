package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class VisibleSessionSelectorTest {

    private final VisibleSessionSelector selector = new VisibleSessionSelector();

    @Test
    public void selectsOnlyCurrentSessionWhenListClosed() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", false,
            Arrays.asList("https://example.test/current", "https://example.test/hidden"), Collections.emptySet());

        Assert.assertEquals(Collections.singleton("https://example.test/current"), visible);
    }

    @Test
    public void excludesHiddenSessionsWhenListClosed() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", false, Collections.emptyList(), Collections.emptySet());

        Assert.assertFalse(visible.contains("https://example.test/hidden"));
    }

    @Test
    public void includesOnScreenListRowsWhenListOpen() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", "https://example.test/on-screen"), Collections.emptySet());

        Assert.assertTrue(visible.contains("https://example.test/current"));
        Assert.assertTrue(visible.contains("https://example.test/on-screen"));
        Assert.assertEquals(2, visible.size());
    }

    @Test
    public void excludesOffScreenListRowsWhenListOpen() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", "https://example.test/on-screen"), Collections.emptySet());

        Assert.assertFalse(visible.contains("https://example.test/off-screen"));
    }

    @Test
    public void becomingVisibleSessionIsSelectedForReconnectAndScan() {
        Set<String> hidden = selector.selectVisibleSessionNames(true,
            "https://example.test/current", false, Collections.emptyList(), Collections.emptySet());
        Assert.assertFalse(hidden.contains("https://example.test/becoming-visible"));

        Set<String> afterBecomingCurrent = selector.selectVisibleSessionNames(true,
            "https://example.test/becoming-visible", false, Collections.emptyList(), Collections.emptySet());
        Assert.assertTrue(afterBecomingCurrent.contains("https://example.test/becoming-visible"));
    }

    @Test
    public void becomingVisibleByScrollingIntoOpenListIsSelected() {
        Set<String> beforeScroll = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Collections.singletonList("https://example.test/current"), Collections.emptySet());
        Assert.assertFalse(beforeScroll.contains("https://example.test/scrolled-in"));

        Set<String> afterScroll = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", "https://example.test/scrolled-in"), Collections.emptySet());
        Assert.assertTrue(afterScroll.contains("https://example.test/scrolled-in"));
    }

    @Test
    public void selectsNoSessionsWhenActivityNotVisible() {
        Set<String> visible = selector.selectVisibleSessionNames(false,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", "https://example.test/on-screen"), Collections.emptySet());

        Assert.assertTrue(visible.isEmpty());
    }

    @Test
    public void toleratesNullCurrentSession() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            null, true, Collections.singletonList("https://example.test/on-screen"), Collections.emptySet());

        Assert.assertEquals(Collections.singleton("https://example.test/on-screen"), visible);
    }

    @Test
    public void ignoresNullOnScreenNames() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", null), Collections.emptySet());

        Assert.assertEquals(Collections.singleton("https://example.test/current"), visible);
    }

    @Test
    public void excludesHiddenOnScreenListRowsFromReconnectAndScan() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Arrays.asList("https://example.test/current", "https://example.test/hidden-row"),
            Collections.singleton("https://example.test/hidden-row"));

        Assert.assertEquals(Collections.singleton("https://example.test/current"), visible);
    }

    @Test
    public void keepsTheCurrentSessionEvenWhenItIsMarkedHidden() {
        Set<String> visible = selector.selectVisibleSessionNames(true,
            "https://example.test/current", true,
            Collections.singletonList("https://example.test/current"),
            Collections.singleton("https://example.test/current"));

        Assert.assertEquals(Collections.singleton("https://example.test/current"), visible);
    }
}
