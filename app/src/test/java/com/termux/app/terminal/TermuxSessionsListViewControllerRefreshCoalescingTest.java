package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TermuxSessionsListViewControllerRefreshCoalescingTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    @Test
    public void refreshDebounceWindowIsTheLongerWindowWhileThePostReconnectWindowIsActive() {
        Assert.assertEquals(
            TermuxSessionsListViewController.POST_RECONNECT_REFRESH_DEBOUNCE_WINDOW_MILLIS,
            TermuxSessionsListViewController.refreshDebounceWindowMillis(true));
    }

    @Test
    public void refreshDebounceWindowIsTheNormalWindowWhenThePostReconnectWindowIsInactive() {
        Assert.assertEquals(
            TermuxSessionsListViewController.REFRESH_DEBOUNCE_WINDOW_MILLIS,
            TermuxSessionsListViewController.refreshDebounceWindowMillis(false));
    }

    @Test
    public void theLongerPostReconnectWindowCoalescesMoreThanTheNormalWindow() {
        Assert.assertTrue("the post-reconnect debounce window must be longer than the normal window "
                + "so the reconnect burst collapses into fewer rebuilds",
            TermuxSessionsListViewController.POST_RECONNECT_REFRESH_DEBOUNCE_WINDOW_MILLIS
                > TermuxSessionsListViewController.REFRESH_DEBOUNCE_WINDOW_MILLIS);
    }

    @Test
    public void rowOrderIsStableAcrossReconnectBurstTierChanges() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("alpha", "beta")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Arrays.asList("gamma")));
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");

        List<Integer> orderBefore = SessionHierarchyBuilder.visibleSessionIndexes(
            builder.build(sessionNames, entries, NA));
        List<Integer> orderAfter = SessionHierarchyBuilder.visibleSessionIndexes(
            builder.build(sessionNames, entries, NA));

        Assert.assertEquals("bottom-sheet row order must derive only from the project/story "
                + "definition hierarchy and session identity, never from volatile activity, tier, or "
                + "reconnecting state, so it must be identical across the reconnect burst",
            orderBefore, orderAfter);
        Assert.assertEquals(Arrays.asList(0, 1, 2), orderAfter);
    }

    @Test
    public void reversedSessionListDoesNotReorderRowsRelativeToTheDefinitionHierarchy() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("alpha", "beta", "gamma")));

        List<Integer> naturalOrderNames = SessionHierarchyBuilder.visibleSessionIndexes(
            builder.build(Arrays.asList("alpha", "beta", "gamma"), entries, NA));

        Assert.assertEquals("the row order follows the definition URL order projected onto the "
                + "session indexes, a stable key independent of any volatile per-session state",
            Arrays.asList(0, 1, 2), naturalOrderNames);
    }
}
