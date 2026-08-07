package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagOccurrence;
import com.termux.app.outputtag.OutputTagScanner;

import java.util.List;

public final class CallToUserTagScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("call-to-user", CallToUserTagScanner::normalizeReason,
            StatuslineCallCycleKey::resolve);

    public static List<String> extractReasons(String output) {
        return new OutputTagScanner("call-to-user", CallToUserTagScanner::normalizeReason).extractValues(output);
    }

    public static String normalizeReason(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    public List<OutputTagOccurrence> newCalls(String output) {
        return outputTagScanner.newOccurrences(output);
    }
}
