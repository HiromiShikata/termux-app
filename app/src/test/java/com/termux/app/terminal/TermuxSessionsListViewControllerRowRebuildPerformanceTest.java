package com.termux.app.terminal;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TermuxSessionsListViewControllerRowRebuildPerformanceTest {

    private static final class CallCountingSessionNewActivityStore extends SessionNewActivityStore {

        final Map<String, Integer> dotTierLookupsByName = new LinkedHashMap<>();

        @Override
        Long outActivityTimeMillisForDotTier(@NonNull String sessionName) {
            Integer previous = dotTierLookupsByName.get(sessionName);
            dotTierLookupsByName.put(sessionName, previous == null ? 1 : previous + 1);
            return super.outActivityTimeMillisForDotTier(sessionName);
        }
    }

    @Test
    public void indicatorBuilderComputesEachVisibleSessionIndicatorExactlyOncePerBuild() {
        CallCountingSessionNewActivityStore store = new CallCountingSessionNewActivityStore();
        store.recordOutputActivity("alpha", 1_000L);
        store.recordOutputActivity("beta", 1_000L);

        Map<Integer, SessionNewActivityIndicator> indicators =
            TermuxSessionsListViewController.sessionActivityIndicatorsByIndex(
                store, Arrays.asList(0, 1), Arrays.asList("alpha", "beta"),
                Collections.emptySet(), 2_000L);

        Assert.assertEquals(2, indicators.size());
        Assert.assertEquals(Integer.valueOf(1), store.dotTierLookupsByName.get("alpha"));
        Assert.assertEquals(Integer.valueOf(1), store.dotTierLookupsByName.get("beta"));
    }

    @Test
    public void indicatorBuilderSkipsDisabledSessionsSoTheyAreNeverQueried() {
        CallCountingSessionNewActivityStore store = new CallCountingSessionNewActivityStore();
        store.recordOutputActivity("alpha", 1_000L);
        store.recordOutputActivity("beta", 1_000L);
        Set<String> disabled = Collections.singleton("beta");

        Map<Integer, SessionNewActivityIndicator> indicators =
            TermuxSessionsListViewController.sessionActivityIndicatorsByIndex(
                store, Arrays.asList(0, 1), Arrays.asList("alpha", "beta"), disabled, 2_000L);

        Assert.assertEquals(Collections.singleton(0), indicators.keySet());
        Assert.assertEquals(Integer.valueOf(1), store.dotTierLookupsByName.get("alpha"));
        Assert.assertNull(store.dotTierLookupsByName.get("beta"));
    }

    @Test
    public void indicatorBuilderOmitsSessionsWithoutVisibleActivitySoTheyCarryNoIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("alpha", 1_000L);

        Map<Integer, SessionNewActivityIndicator> indicators =
            TermuxSessionsListViewController.sessionActivityIndicatorsByIndex(
                store, Arrays.asList(0, 1), Arrays.asList("alpha", "beta"),
                Collections.emptySet(), 2_000L);

        Assert.assertEquals(Collections.singleton(0), indicators.keySet());
    }

    @Test
    public void rowContentComparesEqualWhenBoundTextIsIdenticalSoUnchangedRowsAreNotRebound() {
        SessionRowContent first = new SessionRowContent("worker|out: 1m|RED");
        SessionRowContent second = new SessionRowContent("worker|out: 1m|RED");

        Assert.assertTrue(SessionRowContent.sameContent(first, second));
    }

    @Test
    public void rowContentComparesUnequalWhenBoundTextDiffersSoChangedRowsAreRebound() {
        SessionRowContent before = new SessionRowContent("worker|out: 1m|RED");
        SessionRowContent afterTimesChange = new SessionRowContent("worker|out: 2m|RED");

        Assert.assertFalse(SessionRowContent.sameContent(before, afterTimesChange));
    }

    @Test
    public void rowContentTreatsAMissingSnapshotAsDistinctFromAPresentOne() {
        SessionRowContent present = new SessionRowContent("worker|out: 1m|RED");

        Assert.assertFalse(SessionRowContent.sameContent(present, null));
        Assert.assertFalse(SessionRowContent.sameContent(null, present));
        Assert.assertTrue(SessionRowContent.sameContent(null, null));
    }

    @Test
    public void diffCallbackReportsContentsUnchangedForTheSameSessionWhenItsBoundTextIsStable() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        long itemId = TermuxSessionsListViewController.rowItemId(rows.get(0));
        Map<Long, SessionRowContent> previousContent =
            Collections.singletonMap(itemId, new SessionRowContent("worker|out: 1m"));
        Map<Long, SessionRowContent> currentContent =
            Collections.singletonMap(itemId, new SessionRowContent("worker|out: 1m"));

        TermuxSessionsListViewController.SessionRowDiffCallback callback =
            new TermuxSessionsListViewController.SessionRowDiffCallback(
                rows, rows,
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                previousContent, currentContent);

        Assert.assertTrue(callback.areItemsTheSame(0, 0));
        Assert.assertTrue(callback.areContentsTheSame(0, 0));
    }

    @Test
    public void diffCallbackReportsContentsChangedForTheSameSessionWhenItsBoundTextChanges() {
        List<SessionHierarchyRow> rows = Collections.singletonList(SessionHierarchyRow.session(0));
        long itemId = TermuxSessionsListViewController.rowItemId(rows.get(0));
        Map<Long, SessionRowContent> previousContent =
            Collections.singletonMap(itemId, new SessionRowContent("worker|out: 1m"));
        Map<Long, SessionRowContent> currentContent =
            Collections.singletonMap(itemId, new SessionRowContent("worker|out: 2m"));

        TermuxSessionsListViewController.SessionRowDiffCallback callback =
            new TermuxSessionsListViewController.SessionRowDiffCallback(
                rows, rows,
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                previousContent, currentContent);

        Assert.assertTrue(callback.areItemsTheSame(0, 0));
        Assert.assertFalse(callback.areContentsTheSame(0, 0));
    }

    @Test
    public void diffCallbackReportsProjectHeaderContentChangedWhenItsSessionCountChanges() {
        List<SessionHierarchyRow> rows =
            Collections.singletonList(SessionHierarchyRow.projectHeader("alpha"));
        long itemId = TermuxSessionsListViewController.rowItemId(rows.get(0));
        Map<Long, SessionRowContent> previousContent =
            Collections.singletonMap(itemId, new SessionRowContent("alpha (2)"));
        Map<Long, SessionRowContent> currentContent =
            Collections.singletonMap(itemId, new SessionRowContent("alpha (3)"));

        TermuxSessionsListViewController.SessionRowDiffCallback callback =
            new TermuxSessionsListViewController.SessionRowDiffCallback(
                rows, rows,
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                TermuxSessionsListViewController.assignUniqueRowItemIds(rows),
                previousContent, currentContent);

        Assert.assertFalse(callback.areContentsTheSame(0, 0));
    }
}
