package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects owner calls in one session's rendered transcript.
 *
 * <p>Two tag families appear in that transcript and they carry different halves of a call. The
 * candidate family {@code <call-to-user-pending>} is printed by the agent inside its own message and
 * carries the human-readable reason line; it NEVER fires a call on its own, because a candidate that
 * the approval gate suppresses must not reach the owner. The literal family {@code <call-to-user>} is
 * rendered by the statusline only after that gate approves the message, and it is what fires the
 * call. Its inner text is an OPAQUE trigger value: it identifies the call for deduplication and is
 * never taken as display content, so the app behaves identically whether the emitter puts an
 * approval timestamp or arbitrary message text there.
 *
 * <p>The reason shown for a fired call is the most recent candidate reason this session has streamed,
 * remembered across scans. The candidate is printed earlier in the same rendered message than the
 * statusline line that promotes it, and either of them can scroll out of the rendered window between
 * one scan and the next, so the reason cannot be re-read from the window at promotion time.
 *
 * <p>Binding is by recency in the streamed output, and a remembered reason arms exactly one call.
 * Both families are extracted as a single event sequence in document order and deduplicated by the
 * shared {@link OutputTagScanner} stream logic, so a trigger takes the candidate that precedes it
 * rather than a later one that happens to be visible in the same window. Consuming the remembered
 * reason on use is what keeps a suppressed candidate from leaking: once a call has taken it, a later
 * unrelated call cannot display it again and falls back to its own trigger value, which keeps the
 * call recorded instead of silently dropping it.
 *
 * <p>Document order alone is not enough to bind correctly, because the statusline is redrawn in
 * place at the bottom of the window and its output is refreshed on an interval rather than on every
 * frame. A message printed between two refreshes therefore appears ABOVE a statusline row that still
 * carries the previous call's trigger value, which puts an already-fired trigger after a candidate
 * that became visible later. The stream deduplication cannot absorb that on its own: it reports the
 * genuinely new candidate and everything following it as new, so the repeated trigger is handed back
 * as well. This scanner therefore also remembers which trigger values have already fired, and
 * guarantees that a trigger value which has already fired can never consume a candidate that first
 * became visible after that trigger fired, so a genuine new trigger always consumes the most recent
 * candidate not yet consumed by an earlier trigger. Without it the repeated trigger takes the new
 * candidate's reason and the store then discards the call as an already-known trigger value, so the
 * reason is destroyed and the next genuine call falls back to rendering its raw trigger value.
 *
 * <p>A repeat withholds the remembered candidate reason and nothing else: the call is still handed
 * on, carrying its own trigger value as the display reason. Suppressing the whole call here would
 * make this scanner a second, disagreeing authority on which calls are already known. The store keeps
 * {@link com.termux.app.terminal.session.SessionNewActivityStateCaps#MAX_CALL_TRIGGER_VALUES_PER_SESSION}
 * keys, evicts from the head, and drops the whole key set when a session reconnects in place, so a
 * value this scanner still remembers can be one the store has already forgotten and would have
 * accepted as a new call. Handing the call on leaves the store as the only authority that decides
 * whether a call is already known: a repeat it still recognizes is discarded exactly as before, and
 * one it no longer recognizes reaches the owner instead of disappearing unnoticed.
 */
public final class CallToUserTagScanner {

    private static final String TRIGGER_TAG_NAME = "call-to-user";

    private static final String CANDIDATE_TAG_NAME = "call-to-user-pending";

    /**
     * Matches a complete block of either family, the longer tag name first so a candidate block is
     * never read as a literal block. An opening marker whose closing marker never arrived — what the
     * emitter leaves behind when a long non-ASCII line is cut at the pane width — deliberately does
     * not match, so a partially rendered tag fires nothing and records nothing.
     */
    private static final Pattern TAG_BLOCK_PATTERN =
        Pattern.compile("<(" + CANDIDATE_TAG_NAME + "|" + TRIGGER_TAG_NAME + ")>([\\s\\S]*?)</\\1>");

    private static final String CANDIDATE_EVENT_PREFIX = "candidate ";

    private static final String TRIGGER_EVENT_PREFIX = "trigger ";

    /**
     * How many already-fired trigger values one session remembers, oldest evicted first, so a
     * long-lived session cannot grow without limit. The bound is well above
     * {@link com.termux.app.terminal.session.SessionNewActivityStateCaps#MAX_CALL_TRIGGER_VALUES_PER_SESSION}
     * because a value forgotten here is a repeat that consumes the remembered candidate reason again,
     * which is the cached-statusline defect returning; a value remembered here that the store no
     * longer knows costs only the display reason of that one repeat, which falls back to its trigger
     * value. Overflowing is therefore the more expensive direction and the bound sits far from it.
     */
    static final int MAX_REMEMBERED_TRIGGER_VALUES = 64;

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner(CallToUserTagScanner::extractOrderedEvents);

    /** Trigger values that have already fired in this session, oldest first. */
    private final LinkedHashSet<String> mFiredTriggerValues = new LinkedHashSet<>();

    private String mRememberedCandidateReason;

    public static List<String> extractTriggerValues(String output) {
        return valuesWithPrefix(output, TRIGGER_EVENT_PREFIX);
    }

    public static List<String> extractCandidateReasons(String output) {
        return valuesWithPrefix(output, CANDIDATE_EVENT_PREFIX);
    }

    public static String normalizeReason(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    public List<ApprovedCallToUser> newCalls(String output) {
        List<ApprovedCallToUser> calls = new ArrayList<>();
        for (String event : outputTagScanner.newValues(output)) {
            if (event.startsWith(CANDIDATE_EVENT_PREFIX)) {
                mRememberedCandidateReason = event.substring(CANDIDATE_EVENT_PREFIX.length());
                continue;
            }
            String triggerValue = event.substring(TRIGGER_EVENT_PREFIX.length());
            calls.add(new ApprovedCallToUser(triggerValue, displayReasonFor(triggerValue)));
        }
        return calls;
    }

    /**
     * The reason a call fired by {@code triggerValue} displays. A trigger value firing for the first
     * time consumes the remembered candidate reason, which arms exactly one call. A repeat is the same
     * call rendered again by the cached statusline rather than a new one, so it leaves the remembered
     * reason for the genuine trigger still to come and displays its own trigger value instead.
     */
    private String displayReasonFor(String triggerValue) {
        if (!rememberFiredTriggerValue(triggerValue)) {
            return triggerValue;
        }
        if (mRememberedCandidateReason == null) {
            return triggerValue;
        }
        String displayReason = mRememberedCandidateReason;
        mRememberedCandidateReason = null;
        return displayReason;
    }

    /**
     * Records {@code triggerValue} as fired and reports whether it is the first time it fired.
     * Returning false marks a repeat. The bound below is what makes the answer reliable only for a
     * recent window of trigger values: once a value is evicted, a later repeat of it is reported as a
     * first firing and consumes the remembered candidate reason.
     */
    private boolean rememberFiredTriggerValue(String triggerValue) {
        if (!mFiredTriggerValues.add(triggerValue)) {
            return false;
        }
        Iterator<String> oldestFirst = mFiredTriggerValues.iterator();
        while (mFiredTriggerValues.size() > MAX_REMEMBERED_TRIGGER_VALUES && oldestFirst.hasNext()) {
            oldestFirst.next();
            oldestFirst.remove();
        }
        return true;
    }

    private static List<String> valuesWithPrefix(String output, String eventPrefix) {
        List<String> values = new ArrayList<>();
        for (String event : extractOrderedEvents(output)) {
            if (event.startsWith(eventPrefix))
                values.add(event.substring(eventPrefix.length()));
        }
        return values;
    }

    private static List<String> extractOrderedEvents(String output) {
        List<String> events = new ArrayList<>();
        if (output == null) return events;

        Matcher matcher = TAG_BLOCK_PATTERN.matcher(output);
        while (matcher.find()) {
            String value = normalizeReason(matcher.group(2));
            if (value == null) continue;
            events.add(CANDIDATE_TAG_NAME.equals(matcher.group(1))
                ? CANDIDATE_EVENT_PREFIX + value
                : TRIGGER_EVENT_PREFIX + value);
        }
        return events;
    }
}
