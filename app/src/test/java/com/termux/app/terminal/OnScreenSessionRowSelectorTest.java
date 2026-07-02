package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class OnScreenSessionRowSelectorTest {

    private final OnScreenSessionRowSelector selector = new OnScreenSessionRowSelector();

    private static final List<SessionHierarchyRow> ROWS = Arrays.asList(
        SessionHierarchyRow.projectHeader("project"),
        SessionHierarchyRow.session(0, "https://example.test/a"),
        SessionHierarchyRow.session(1, "https://example.test/b"),
        SessionHierarchyRow.session(2, "https://example.test/c"),
        SessionHierarchyRow.session(3, "https://example.test/d"));

    @Test
    public void returnsOnlySessionNamesWithinOnScreenRange() {
        List<String> onScreen = selector.selectOnScreenSessionNames(ROWS, 1, 2);

        Assert.assertEquals(Arrays.asList("https://example.test/a", "https://example.test/b"), onScreen);
    }

    @Test
    public void skipsHeaderRowsInRange() {
        List<String> onScreen = selector.selectOnScreenSessionNames(ROWS, 0, 1);

        Assert.assertEquals(Arrays.asList("https://example.test/a"), onScreen);
    }

    @Test
    public void excludesOffScreenSessionRows() {
        List<String> onScreen = selector.selectOnScreenSessionNames(ROWS, 1, 1);

        Assert.assertEquals(Arrays.asList("https://example.test/a"), onScreen);
        Assert.assertFalse(onScreen.contains("https://example.test/b"));
        Assert.assertFalse(onScreen.contains("https://example.test/d"));
    }

    @Test
    public void returnsEmptyWhenNoRowsOnScreen() {
        List<String> onScreen = selector.selectOnScreenSessionNames(ROWS,
            OnScreenSessionRowSelector.NO_POSITION, OnScreenSessionRowSelector.NO_POSITION);

        Assert.assertTrue(onScreen.isEmpty());
    }

    @Test
    public void clampsLastPositionToRowCount() {
        List<String> onScreen = selector.selectOnScreenSessionNames(ROWS, 3, 99);

        Assert.assertEquals(Arrays.asList("https://example.test/c", "https://example.test/d"), onScreen);
    }
}
