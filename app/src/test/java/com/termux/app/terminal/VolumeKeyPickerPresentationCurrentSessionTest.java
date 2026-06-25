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
        private final SessionNewActivityStore newActivityStore;
        private int currentSessionIndex;
        private long nowMillis;

        FakeSessionRuntime(List<String> sessionNames, SessionNewActivityStore newActivityStore,
                           int currentSessionIndex, long nowMillis) {
            this.sessionNames = sessionNames;
            this.newActivityStore = newActivityStore;
            this.currentSessionIndex = currentSessionIndex;
            this.nowMillis = nowMillis;
            recordSeenForCurrentSession();
        }

        void switchTo(int sessionIndex) {
            this.currentSessionIndex = sessionIndex;
            recordSeenForCurrentSession();
        }

        void replyToCurrentSession() {
            newActivityStore.recordUserInput(sessionNames.get(currentSessionIndex), nowMillis);
        }

        private void recordSeenForCurrentSession() {
            newActivityStore.recordSeen(sessionNames.get(currentSessionIndex), nowMillis);
        }

        Map<Integer, SessionRow> renderModelSessionRows() {
            Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
            Map<Integer, String> ageLabelsByIndex = new LinkedHashMap<>();
            for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
                SessionNewActivityIndicator indicator = TermuxSessionsListViewController.newActivityIndicator(
                    newActivityStore, sessionNames.get(sessionIndex), nowMillis);
                if (indicator.isVisible()) {
                    tiersByIndex.put(sessionIndex, indicator.getTier());
                    ageLabelsByIndex.put(sessionIndex, indicator.getLabel());
                }
            }
            return SessionRow.project(sessionNames, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                tiersByIndex, ageLabelsByIndex, Collections.emptySet(), currentSessionIndex);
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
    public void immediateSwitchPickerMarksTheJustSwitchedSessionAsCurrentAndKeepsItsRedDotUntilReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-1", 1_000L);
        FakeSessionRuntime runtime = new FakeSessionRuntime(
            Arrays.asList("session-0", "session-1"), store, 0, 4_000L);
        store.recordExplicitCall("session-0", 4_500L);

        List<SessionPickerOverlayLine> lines = presentAndCaptureRenderedLines(runtime, 1, true);

        Assert.assertTrue(lines.get(1).isCurrent());
        Assert.assertTrue(lines.get(1).isMarked());

        Assert.assertFalse(lines.get(0).isCurrent());
        Assert.assertTrue(lines.get(0).isMarked());

        runtime.replyToCurrentSession();
        List<SessionPickerOverlayLine> afterReply = presentAndCaptureRenderedLines(runtime, 1, true);
        Assert.assertFalse(afterReply.get(1).isMarked());
        Assert.assertEquals("", afterReply.get(1).getNewActivityLabel());
    }

    @Test
    public void previewFirstPickerMarksTheStillActiveSessionAsCurrentAndKeepsItsRedDotUntilReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-0", 1_000L);
        store.recordExplicitCall("session-1", 1_000L);
        FakeSessionRuntime runtime = new FakeSessionRuntime(
            Arrays.asList("session-0", "session-1"), store, 0, 4_000L);

        List<SessionPickerOverlayLine> lines = presentAndCaptureRenderedLines(runtime, 1, false);

        Assert.assertTrue(lines.get(0).isCurrent());
        Assert.assertTrue(lines.get(0).isMarked());
        Assert.assertFalse(lines.get(1).isCurrent());
        Assert.assertTrue(lines.get(1).isMarked());

        runtime.replyToCurrentSession();
        List<SessionPickerOverlayLine> afterReply = presentAndCaptureRenderedLines(runtime, 1, false);
        Assert.assertFalse(afterReply.get(0).isMarked());
    }
}
