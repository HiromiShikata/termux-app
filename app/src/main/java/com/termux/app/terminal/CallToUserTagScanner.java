package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.ArrayList;
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

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner(CallToUserTagScanner::extractOrderedEvents);

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
            String displayReason =
                mRememberedCandidateReason == null ? triggerValue : mRememberedCandidateReason;
            mRememberedCandidateReason = null;
            calls.add(new ApprovedCallToUser(triggerValue, displayReason));
        }
        return calls;
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
