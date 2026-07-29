package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.termux.app.terminal.session.SessionNewActivityStateCaps;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * The scanner remembers which trigger values have already fired so that a statusline row still
 * carrying a previous call's trigger value cannot consume a candidate reason printed after it. That
 * memory is bounded and is a different set from the store's deduplication key set, so these cover
 * what the two authorities do where they disagree: a repeat withholds the candidate reason and
 * nothing else, so no divergence can make a call disappear, and past the bound the repeat is no
 * longer recognized and the cached-statusline defect returns.
 */
public class CallToUserRepeatedTriggerValueTest {

    private static final String SESSION = "session-one";

    private static final String FIRST_TRIGGER_VALUE = "2026-07-28T09:40:00.000Z";

    /**
     * The bound these cases pin, stated here independently of the production constant. Deriving it
     * from {@link CallToUserTagScanner#MAX_REMEMBERED_TRIGGER_VALUES} would make every case scale
     * with whatever that constant becomes and assert nothing about its value.
     */
    private static final int PINNED_REMEMBERED_TRIGGER_VALUE_BOUND = 64;

    private static final class RecordingTrigger implements CallToUserTagController.CallTrigger {
        final List<String> triggerValues = new ArrayList<>();
        final List<String> reasons = new ArrayList<>();

        @Override
        public void onCallToUser(String sessionKey, String triggerValue, String reason) {
            triggerValues.add(triggerValue);
            reasons.add(reason);
        }
    }

    private static final long FIRST_CALL_TIME_MILLIS = 5_000L;

    private static final long LATER_CALL_TIME_MILLIS = 9_000L;

    private static final class AdvancingClock {
        long nowMillis = FIRST_CALL_TIME_MILLIS;
    }

    private static CallToUserTagController controllerInto(SessionNewActivityStore store,
                                                          AdvancingClock clock) {
        return new CallToUserTagController((sessionKey, triggerValue, reason) ->
            store.recordExplicitCall(sessionKey, clock.nowMillis, triggerValue, reason));
    }

    private static String render(String candidateReason, String triggerValue) {
        return "<call-to-user-pending>" + candidateReason + "</call-to-user-pending>\n"
            + "<call-to-user>" + triggerValue + "</call-to-user>\n";
    }

    private static String distinctTriggerValue(int index) {
        return String.format("2026-07-28T10:%02d:%02d.000Z", index / 60, index % 60);
    }

    private static void fireDistinctCalls(CallToUserTagController controller, int count) {
        for (int index = 0; index < count; index++) {
            controller.onSessionTextChanged(SESSION,
                render("filler ask " + index, distinctTriggerValue(index)));
        }
    }

    /**
     * The measured shape of the defect this narrowing removes. The store keeps
     * {@link com.termux.app.terminal.session.SessionNewActivityStateCaps#MAX_CALL_TRIGGER_VALUES_PER_SESSION}
     * keys and evicts from the head, so after enough further calls it has forgotten this trigger
     * value and would accept a call carrying it. A scanner that suppressed the whole call would
     * decide that on the store's behalf and the owner would never learn the message existed.
     */
    @Test
    public void aCallReusingATriggerValueTheStoreHasEvictedStillReachesTheStore() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        AdvancingClock clock = new AdvancingClock();
        CallToUserTagController controller = controllerInto(store, clock);

        controller.onSessionTextChanged(SESSION, render("first ask", FIRST_TRIGGER_VALUE));
        fireDistinctCalls(controller,
            SessionNewActivityStateCaps.MAX_CALL_TRIGGER_VALUES_PER_SESSION);
        assertEquals(Long.valueOf(FIRST_CALL_TIME_MILLIS),
            store.getLastExplicitCallTimeMillis(SESSION));

        clock.nowMillis = LATER_CALL_TIME_MILLIS;
        controller.onSessionTextChanged(SESSION, render("later ask", FIRST_TRIGGER_VALUE));

        assertEquals(Long.valueOf(LATER_CALL_TIME_MILLIS),
            store.getLastExplicitCallTimeMillis(SESSION));
    }

    /**
     * The store drops a session's whole key set in place while the scanner keeps its own memory, so
     * matching the two bounds would not make the two sets agree. A scanner that suppressed the whole
     * call would drop a call the store was ready to accept; withholding only the candidate reason
     * leaves the store the single authority on what is already known.
     */
    @Test
    public void aRepeatedTriggerValueIsDeliveredAfterTheStoreDroppedItsKeySet() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        AdvancingClock clock = new AdvancingClock();
        CallToUserTagController controller = controllerInto(store, clock);

        controller.onSessionTextChanged(SESSION, render("first ask", FIRST_TRIGGER_VALUE));
        assertEquals(Long.valueOf(FIRST_CALL_TIME_MILLIS),
            store.getLastExplicitCallTimeMillis(SESSION));

        store.purgeSessionPreservingStatuslineTimes(SESSION);

        clock.nowMillis = LATER_CALL_TIME_MILLIS;
        controller.onSessionTextChanged(SESSION, render("later ask", FIRST_TRIGGER_VALUE));

        assertEquals(Long.valueOf(LATER_CALL_TIME_MILLIS),
            store.getLastExplicitCallTimeMillis(SESSION));
    }

    /**
     * A repeat the store still recognizes changes nothing the owner sees: the store discards it, and
     * the candidate printed above the stale statusline row survives for the genuine trigger that
     * follows. This is the cached-statusline fix, restated against the narrowed guard.
     */
    @Test
    public void aRepeatTheStoreStillRecognizesIsDiscardedAndLeavesTheCandidateForTheNextTrigger() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerInto(store, new AdvancingClock());

        controller.onSessionTextChanged(SESSION, render("first ask", FIRST_TRIGGER_VALUE));
        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask</call-to-user-pending>\n"
                + "<call-to-user-pending>second ask</call-to-user-pending>\n"
                + "<call-to-user>" + FIRST_TRIGGER_VALUE + "</call-to-user>\n");
        controller.onSessionTextChanged(SESSION,
            "<call-to-user-pending>first ask</call-to-user-pending>\n"
                + "<call-to-user-pending>second ask</call-to-user-pending>\n"
                + "<call-to-user>2026-07-28T09:55:00.000Z</call-to-user>\n");

        assertEquals(List.of("first ask", "second ask"), store.getUnacknowledgedCallReasons(SESSION));
    }

    /**
     * Pins the bound from below. With exactly {@link #PINNED_REMEMBERED_TRIGGER_VALUE_BOUND}
     * distinct trigger values fired, the oldest is still remembered, so repeating it takes no
     * candidate reason and the genuine trigger that follows still carries its own.
     */
    @Test
    public void theOldestTriggerValueStillInsideTheBoundTakesNoCandidateReason() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION, render("first ask", FIRST_TRIGGER_VALUE));
        fireDistinctCalls(controller, PINNED_REMEMBERED_TRIGGER_VALUE_BOUND - 1);

        controller.onSessionTextChanged(SESSION, render("later ask", FIRST_TRIGGER_VALUE));
        controller.onSessionTextChanged(SESSION,
            "<call-to-user>2026-07-28T11:30:00.000Z</call-to-user>\n");

        int lastIndex = trigger.reasons.size() - 1;
        assertEquals("2026-07-28T11:30:00.000Z", trigger.triggerValues.get(lastIndex));
        assertEquals("later ask", trigger.reasons.get(lastIndex));
        assertEquals(FIRST_TRIGGER_VALUE, trigger.reasons.get(lastIndex - 1));
    }

    /**
     * Pins the bound from above, and states the price of overflowing it. One distinct trigger value
     * past {@link #PINNED_REMEMBERED_TRIGGER_VALUE_BOUND} evicts the oldest, so repeating
     * it is read as a first firing, it consumes the candidate printed above the stale statusline row,
     * and the genuine trigger that follows falls back to rendering its own raw trigger value — the
     * cached-statusline defect, returning exactly past the bound.
     */
    @Test
    public void aTriggerValueEvictedPastTheBoundConsumesTheLaterCandidateAgain() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(SESSION, render("first ask", FIRST_TRIGGER_VALUE));
        fireDistinctCalls(controller, PINNED_REMEMBERED_TRIGGER_VALUE_BOUND);

        controller.onSessionTextChanged(SESSION, render("later ask", FIRST_TRIGGER_VALUE));
        controller.onSessionTextChanged(SESSION,
            "<call-to-user>2026-07-28T11:30:00.000Z</call-to-user>\n");

        int lastIndex = trigger.reasons.size() - 1;
        assertEquals("2026-07-28T11:30:00.000Z", trigger.triggerValues.get(lastIndex));
        assertEquals("2026-07-28T11:30:00.000Z", trigger.reasons.get(lastIndex));
        assertEquals("later ask", trigger.reasons.get(lastIndex - 1));
    }

    @Test
    public void everyDistinctTriggerValueUsedByTheBoundCasesIsUnique() {
        List<String> seen = new ArrayList<>();
        for (int index = 0; index <= PINNED_REMEMBERED_TRIGGER_VALUE_BOUND; index++) {
            String value = distinctTriggerValue(index);
            assertTrue("repeated filler trigger value at index " + index, !seen.contains(value));
            seen.add(value);
        }
        assertTrue(!seen.contains(FIRST_TRIGGER_VALUE));
    }
}
