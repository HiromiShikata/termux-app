package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * The displayed pending-call reason comes from the candidate {@code <call-to-user-pending>} tag the
 * agent prints, while the call itself keeps firing on the literal {@code <call-to-user>} tag the
 * statusline renders once the approval gate passes. The literal tag's inner value is an approval
 * timestamp that is distinct per call, so it is the deduplication key; the reason is display-only
 * payload and is no longer unique per call.
 */
public class CallToUserCandidateReasonTest {

    private static final String SESSION = "session-one";

    private static final class RecordingTrigger implements CallToUserTagController.CallTrigger {
        final List<String> triggerValues = new ArrayList<>();
        final List<String> reasons = new ArrayList<>();

        @Override
        public void onCallToUser(String sessionKey, String triggerValue, String reason) {
            triggerValues.add(triggerValue);
            reasons.add(reason);
        }
    }

    private static CallToUserTagController controllerInto(SessionNewActivityStore store,
                                                          long fixedTimeMillis) {
        return new CallToUserTagController((sessionKey, triggerValue, reason) ->
            store.recordExplicitCall(sessionKey, fixedTimeMillis, triggerValue, reason));
    }

    @Test
    public void displaysTheCandidateReasonRatherThanTheApprovalTimestampCarriedByTheLiteralTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>🔴deploy review: please approve</call-to-user-pending>\n"
                + "2026-07-28T09:00:00Z\n"
                + "<call-to-user>2026-07-28T09:00:01.234Z</call-to-user>\n");

        assertEquals(1, trigger.reasons.size());
        assertEquals("2026-07-28T09:00:01.234Z", trigger.triggerValues.get(0));
        assertEquals("🔴deploy review: please approve", trigger.reasons.get(0));
    }

    @Test
    public void remembersACandidateReasonThatScrolledOutOfTheWindowBeforeThePromotionRender() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>release cut: confirm the tag</call-to-user-pending>\n");
        assertTrue(trigger.reasons.isEmpty());

        controller.onSessionTextChanged(SESSION,
            "later output that scrolled the candidate away\n"
                + "<call-to-user>2026-07-28T09:05:00.000Z</call-to-user>\n");

        assertEquals(1, trigger.reasons.size());
        assertEquals("release cut: confirm the tag", trigger.reasons.get(0));
    }

    @Test
    public void secondCallCarryingTheSameReasonTextStillRegistersAndReArmsThePendingState() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordExplicitCall(SESSION, 1_000L, "2026-07-28T09:00:00.000Z", "status report");
        assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        store.recordUserInput(SESSION, 2_000L);
        assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
        assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());

        store.recordExplicitCall(SESSION, 3_000L, "2026-07-28T09:10:00.000Z", "status report");

        assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        assertEquals(List.of("status report"), store.getUnacknowledgedCallReasons(SESSION));
        assertEquals(Long.valueOf(3_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void reScanOfTheSameApprovalTimestampStaysANoOpEvenWhenTheReasonDiffers() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordExplicitCall(SESSION, 1_000L, "2026-07-28T09:00:00.000Z", "first rendering");
        store.recordExplicitCall(SESSION, 9_000L, "2026-07-28T09:00:00.000Z", "reflowed rendering");

        assertEquals(List.of("first rendering"), store.getUnacknowledgedCallReasons(SESSION));
        assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void aRestartedStoreStillRecognizesAnAlreadyRecordedTriggerValue() {
        InMemorySessionNewActivityPersistence persistence =
            new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall(SESSION, 1_000L, "2026-07-28T09:00:00.000Z", "status report");
        store.recordUserInput(SESSION, 2_000L);

        SessionNewActivityStore restartedStore = new SessionNewActivityStore(persistence);
        restartedStore.recordExplicitCall(SESSION, 9_000L, "2026-07-28T09:00:00.000Z",
            "status report");

        assertEquals(SessionNewActivityTier.NONE, restartedStore.tierFor(SESSION));
        assertTrue(restartedStore.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }

    @Test
    public void callWithNoObservedCandidateReasonStillRegistersUsingTheTriggerValueAsTheReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerInto(store, 5_000L);

        controller.onSessionTextChanged(SESSION,
            "plain session output with no statusline signal\n"
                + "<call-to-user>2026-07-28T09:20:00.000Z</call-to-user>\n");

        assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        assertEquals(List.of("2026-07-28T09:20:00.000Z"),
            store.getUnacknowledgedCallReasons(SESSION));
    }

    @Test
    public void candidateTagAloneNeverFiresACall() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>progress note: still working</call-to-user-pending>\n");

        assertTrue(trigger.reasons.isEmpty());
    }

    @Test
    public void laterApprovedCallFromADifferentMessageDoesNotDisplayASuppressedCandidateReason() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>progress note: still working</call-to-user-pending>\n");
        assertTrue(trigger.reasons.isEmpty());

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>progress note: still working</call-to-user-pending>\n"
                + "intervening work output\n"
                + "<call-to-user-pending>build failure: decide whether to roll back</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:30:00.000Z</call-to-user>\n");

        assertEquals(1, trigger.reasons.size());
        assertEquals("build failure: decide whether to roll back", trigger.reasons.get(0));
    }

    @Test
    public void aRememberedCandidateReasonArmsExactlyOneCallAndIsNotReusedByTheNextCall() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n");
        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n"
                + "<call-to-user>2026-07-28T09:50:00.000Z</call-to-user>\n");

        assertEquals(2, trigger.reasons.size());
        assertEquals("first ask: approve the rollout", trigger.reasons.get(0));
        assertEquals("2026-07-28T09:50:00.000Z", trigger.reasons.get(1));
    }

    @Test
    public void bindsEachTriggerToTheCandidateThatPrecedesItWithinASingleScan() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>older ask: confirm the plan</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T08:00:00.000Z</call-to-user>\n"
                + "work output\n"
                + "<call-to-user-pending>newer ask: confirm the release</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T08:30:00.000Z</call-to-user>\n");

        assertEquals(2, trigger.reasons.size());
        assertEquals("older ask: confirm the plan", trigger.reasons.get(0));
        assertEquals("newer ask: confirm the release", trigger.reasons.get(1));
    }

    /**
     * The statusline is redrawn in place at the bottom of the window and its output is refreshed on
     * an interval rather than on every frame, so a message printed between two refreshes appears
     * ABOVE a statusline row that still carries the previous call's trigger value. The already-fired
     * trigger must not consume the candidate printed after it: doing so destroys that candidate's
     * reason, because the store discards the repeated trigger value as an already-known call. The
     * repeat is still handed on, carrying its own trigger value as the display reason, so the store
     * rather than the scanner decides whether the call is already known.
     */
    @Test
    public void anAlreadyFiredTriggerRepeatedByTheCachedStatuslineDoesNotConsumeALaterCandidate() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n");

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user-pending>second ask: approve the hotfix</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n");

        assertEquals(List.of("2026-07-28T09:40:00.000Z", "2026-07-28T09:40:00.000Z"),
            trigger.triggerValues);
        assertEquals(List.of("first ask: approve the rollout", "2026-07-28T09:40:00.000Z"),
            trigger.reasons);
    }

    /**
     * End to end over the three renders the cached statusline produces: the first call fires, the
     * second candidate is printed while the statusline still shows the first call's trigger value,
     * and the second call's own trigger value arrives on the next refresh. The second call must
     * carry its own reason; falling back to the trigger value would put a raw ISO-8601 timestamp in
     * the pending-call footer, which is the defect this change exists to remove.
     */
    @Test
    public void secondCallKeepsItsOwnReasonAfterTheCachedStatuslineRepeatedTheEarlierTrigger() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerInto(store, 5_000L);

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n");

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user-pending>second ask: approve the hotfix</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:40:00.000Z</call-to-user>\n");

        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask: approve the rollout</call-to-user-pending>\n"
                + "<call-to-user-pending>second ask: approve the hotfix</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:55:00.000Z</call-to-user>\n");

        assertEquals(List.of("first ask: approve the rollout", "second ask: approve the hotfix"),
            store.getUnacknowledgedCallReasons(SESSION));
        for (String reason : store.getUnacknowledgedCallReasons(SESSION)) {
            assertFalse("the footer fell back to a raw trigger value: " + reason,
                reason.startsWith("2026-"));
        }
    }

    @Test
    public void tracksTheRememberedCandidateReasonPerSession() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-a",
            "<call-to-user-pending>session a ask</call-to-user-pending>\n");
        controller.onSessionTextChanged("session-b",
            "<call-to-user>2026-07-28T10:00:00.000Z</call-to-user>\n");

        assertEquals(1, trigger.reasons.size());
        assertEquals("2026-07-28T10:00:00.000Z", trigger.reasons.get(0));
    }
}
