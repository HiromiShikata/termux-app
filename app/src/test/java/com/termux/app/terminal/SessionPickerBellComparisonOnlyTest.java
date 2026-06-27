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
    public void currentSessionWithUnseenSignalStillShowsDotSinceSuppressionIsRemoved() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("current", 5_000L, "needs approval");

        List<SessionPickerOverlayLine> lines = render(store,
            Arrays.asList("current", "background"), 0, 5_500L);

        Assert.assertTrue(lines.get(0).isCurrent());
        Assert.assertTrue(lines.get(0).isMarked());
        Assert.assertEquals(SessionNewActivityTier.RED, lines.get(0).getTier());
    }

    @Test
    public void redIsClearedByUserReplyWhileOutputOnlySessionStaysYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("alpha", 5_000L, "needs approval");
        store.recordUserInput("alpha", 6_000L);
        store.recordOutputActivity("beta", 5_000L);

        List<SessionPickerOverlayLine> lines = render(store,
            Arrays.asList("alpha", "beta"), -1, 6_500L);

        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertTrue(lines.get(1).isMarked());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, lines.get(1).getTier());
    }

    @Test
    public void advancingLastSeenViaSeenTickDoesNotRemoveTheRedDotButReplyingDoes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("current", 5_000L, "needs approval");

        List<SessionPickerOverlayLine> beforeTick = render(store,
            Collections.singletonList("current"), 0, 5_500L);
        Assert.assertTrue(beforeTick.get(0).isMarked());

        store.recordSeen("current", 6_000L);

        List<SessionPickerOverlayLine> afterTick = render(store,
            Collections.singletonList("current"), 0, 6_500L);
        Assert.assertTrue(afterTick.get(0).isMarked());

        store.recordUserInput("current", 7_000L);

        List<SessionPickerOverlayLine> afterReply = render(store,
            Collections.singletonList("current"), 0, 7_500L);
        Assert.assertFalse(afterReply.get(0).isMarked());
    }

    private static List<SessionPickerOverlayLine> render(SessionNewActivityStore store,
                                                         List<String> sessionNames,
                                                         int currentSessionIndex, long nowMillis) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        Map<Integer, String> ageLabelsByIndex = new LinkedHashMap<>();
        List<SessionHierarchyRow> rows = new ArrayList<>(sessionNames.size());
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                store, sessionNames.get(sessionIndex), nowMillis);
            if (indicator.isVisible()) {
                tiersByIndex.put(sessionIndex, indicator.getTier());
                ageLabelsByIndex.put(sessionIndex, indicator.getLabel());
            }
            rows.add(SessionHierarchyRow.session(sessionIndex));
        }
        Map<Integer, SessionRow> sessionRowsByIndex = SessionRow.project(sessionNames,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            tiersByIndex, ageLabelsByIndex, Collections.emptySet(), currentSessionIndex);
        return SessionPickerOverlayRenderModel.build(rows, sessionRowsByIndex, -1);
    }
}
