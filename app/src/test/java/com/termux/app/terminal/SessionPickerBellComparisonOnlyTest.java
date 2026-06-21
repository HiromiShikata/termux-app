package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionPickerBellComparisonOnlyTest {

    @Test
    public void currentSessionWithUnseenBellStillShowsBellSinceSuppressionIsRemoved() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("current", 5_000L);

        List<SessionPickerOverlayLine> lines = render(store,
            Arrays.asList("current", "background"), 0, 5_500L);

        Assert.assertTrue(lines.get(0).isCurrent());
        Assert.assertTrue(lines.get(0).isMarked());
    }

    @Test
    public void renderedBellMatchesLastBellGreaterThanLastSeenForEverySession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("alpha", 5_000L);
        store.recordSeen("alpha", 6_000L);
        store.recordBell("beta", 5_000L);

        List<SessionPickerOverlayLine> lines = render(store,
            Arrays.asList("alpha", "beta"), -1, 6_500L);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertTrue(lines.get(1).isMarked());
    }

    @Test
    public void advancingLastSeenViaSeenTickRemovesTheBellWithoutAnyExplicitClear() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("current", 5_000L);

        List<SessionPickerOverlayLine> beforeTick = render(store,
            Collections.singletonList("current"), 0, 5_500L);
        Assert.assertTrue(beforeTick.get(0).isMarked());

        store.recordSeen("current", 6_000L);

        List<SessionPickerOverlayLine> afterTick = render(store,
            Collections.singletonList("current"), 0, 6_500L);
        Assert.assertFalse(afterTick.get(0).isMarked());
    }

    private static List<SessionPickerOverlayLine> render(SessionNewActivityStore store,
                                                         List<String> sessionNames,
                                                         int currentSessionIndex, long nowMillis) {
        Map<Integer, String> bellAgeLabelsByIndex = new LinkedHashMap<>();
        List<SessionHierarchyRow> rows = new ArrayList<>(sessionNames.size());
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                store, sessionNames.get(sessionIndex), nowMillis);
            if (indicator.isVisible()) {
                bellAgeLabelsByIndex.put(sessionIndex, indicator.getLabel());
            }
            rows.add(SessionHierarchyRow.session(sessionIndex));
        }
        Map<Integer, SessionRow> sessionRowsByIndex = SessionRow.project(sessionNames,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            bellAgeLabelsByIndex, Collections.emptySet(), currentSessionIndex);
        return SessionPickerOverlayRenderModel.build(rows, sessionRowsByIndex, -1);
    }
}
