package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts owner-call reasons from a session transcript.
 *
 * <p>Two tag families carry an owner call and both MUST be recognised here:
 *
 * <ul>
 *   <li>{@code call-to-user} — the legacy tag. It is emitted only when the agent already decided to
 *       call the owner, so matching it on render is correct.
 *   <li>{@code call-to-user-pending} — the candidate tag that agents emit today. It is deliberately
 *       spelled so it does NOT match the legacy pattern, because whether it becomes a real owner
 *       call is decided after render by the agent-side Stop hook, which records its verdict in the
 *       statusline {@code call:} token. Recognising it here does not bypass that verdict: the only
 *       caller, {@link BackgroundOutputTagScanner#scan}, runs this scan only when the session's
 *       statusline {@code call:} token is newer than its {@code reply:} token (or when the session
 *       has no statusline at all), so a candidate tag the hook suppressed never reaches this class.
 * </ul>
 *
 * <p>Before this class recognised the candidate family, a session that emitted only the candidate
 * tag produced no reason match at all, so {@link CallToUserTagController} never invoked its call
 * trigger and the owner was never notified even though the statusline reported a pending call.
 */
public final class CallToUserTagScanner {

    private static final String[] TAG_NAMES = {"call-to-user", "call-to-user-pending"};

    private final List<OutputTagScanner> outputTagScanners = newScanners();

    private static List<OutputTagScanner> newScanners() {
        List<OutputTagScanner> scanners = new ArrayList<>(TAG_NAMES.length);
        for (String tagName : TAG_NAMES) {
            scanners.add(new OutputTagScanner(tagName, CallToUserTagScanner::normalizeReason));
        }
        return scanners;
    }

    public static List<String> extractReasons(String output) {
        List<String> reasons = new ArrayList<>();
        for (OutputTagScanner scanner : newScanners()) {
            reasons.addAll(scanner.extractValues(output));
        }
        return reasons;
    }

    public static String normalizeReason(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    public List<String> newReasons(String output) {
        List<String> reasons = new ArrayList<>();
        for (OutputTagScanner scanner : outputTagScanners) {
            reasons.addAll(scanner.newValues(output));
        }
        return reasons;
    }
}
