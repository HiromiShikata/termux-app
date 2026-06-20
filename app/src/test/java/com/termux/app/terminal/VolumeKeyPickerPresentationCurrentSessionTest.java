package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VolumeKeyPickerPresentationCurrentSessionTest {

    private static final class FakeSessionRuntime {

        private final List<String> sessionNames;
        private final List<String> activeSessionHandles;
        private final SessionNewActivityStore newActivityStore;
        private int currentSessionIndex;
        private long nowMillis;

        FakeSessionRuntime(List<String> sessionNames, List<String> activeSessionHandles,
                           SessionNewActivityStore newActivityStore, int currentSessionIndex, long nowMillis) {
            this.sessionNames = sessionNames;
            this.activeSessionHandles = activeSessionHandles;
            this.newActivityStore = newActivityStore;
            this.currentSessionIndex = currentSessionIndex;
            this.nowMillis = nowMillis;
        }

        void switchTo(int sessionIndex) {
            this.currentSessionIndex = sessionIndex;
        }

        Map<Integer, SessionRow> renderModelSessionRows() {
            Map<Integer, String> markedSessionAgeLabels = new LinkedHashMap<>();
            String activeSessionHandle = activeSessionHandles.get(currentSessionIndex);
            for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
                SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                    newActivityStore, activeSessionHandles.get(sessionIndex), activeSessionHandle, nowMillis);
                if (indicator.isVisible()) {
                    markedSessionAgeLabels.put(sessionIndex, indicator.getLabel());
                }
            }
            return SessionRow.project(sessionNames, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                markedSessionAgeLabels, Collections.emptySet(), currentSessionIndex);
        }
    }

    private static List<SessionPickerOverlayLine> presentAndCaptureRenderedLines(
        FakeSessionRuntime runtime, int highlightedSessionIndex, boolean switchImmediately) {

        List<SessionHierarchyRow> rows = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < 2; sessionIndex++) {
            rows.add(SessionHierarchyRow.session(sessionIndex));
        }
        VolumeKeyPickerMoveDecision decision = switchImmediately
            ? VolumeKeyPickerMoveDecision.decide(false, false, -1, runtime.currentSessionIndex,
                Arrays.asList(0, 1), Arrays.asList(0, 1), highlightedSessionIndex == 1)
            : VolumeKeyPickerMoveDecision.decide(true, false, -1, runtime.currentSessionIndex,
                Arrays.asList(0, 1), Arrays.asList(0, 1), highlightedSessionIndex == 1);

        List<List<SessionPickerOverlayLine>> renderedLinesHolder = new ArrayList<>();
        VolumeKeyPickerPresentation.present(
            decision,
            () -> runtime.switchTo(decision.getHighlightedSessionIndex()),
            () -> renderedLinesHolder.add(SessionPickerOverlayRenderModel.build(
                rows, runtime.renderModelSessionRows(), decision.getHighlightedSessionIndex())),
            () -> { },
            () -> { },
            () -> { });
        return renderedLinesHolder.get(0);
    }

    @Test
    public void immediateSwitchPickerMarksTheJustSwitchedSessionAsCurrentAndSuppressesItsBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-0", 1_000L);
        store.recordBell("handle-1", 1_000L);
        FakeSessionRuntime runtime = new FakeSessionRuntime(
            Arrays.asList("session-0", "session-1"),
            Arrays.asList("handle-0", "handle-1"),
            store, 0, 4_000L);

        List<SessionPickerOverlayLine> lines = presentAndCaptureRenderedLines(runtime, 1, true);

        Assert.assertTrue(lines.get(1).isCurrent());
        Assert.assertFalse(lines.get(1).isMarked());
        Assert.assertEquals("", lines.get(1).getNewActivityLabel());

        Assert.assertFalse(lines.get(0).isCurrent());
        Assert.assertTrue(lines.get(0).isMarked());
    }

    @Test
    public void previewFirstPickerMarksTheStillActiveSessionAsCurrentBeforeAnyCommit() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-0", 1_000L);
        store.recordBell("handle-1", 1_000L);
        FakeSessionRuntime runtime = new FakeSessionRuntime(
            Arrays.asList("session-0", "session-1"),
            Arrays.asList("handle-0", "handle-1"),
            store, 0, 4_000L);

        List<SessionPickerOverlayLine> lines = presentAndCaptureRenderedLines(runtime, 1, false);

        Assert.assertTrue(lines.get(0).isCurrent());
        Assert.assertFalse(lines.get(0).isMarked());
        Assert.assertFalse(lines.get(1).isCurrent());
        Assert.assertTrue(lines.get(1).isMarked());
    }
}
