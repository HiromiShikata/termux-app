package com.termux.app.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CallToUserTagScanner {

    private static final Pattern CALL_TO_USER_BLOCK_PATTERN =
        Pattern.compile("<call-to-user>([\\s\\S]*?)</call-to-user>");

    private int firedReasonCount;

    public static List<String> extractReasons(String output) {
        List<String> reasons = new ArrayList<>();
        if (output == null) return reasons;

        Matcher matcher = CALL_TO_USER_BLOCK_PATTERN.matcher(output);
        while (matcher.find()) {
            String reason = normalizeReason(matcher.group(1));
            if (reason != null) reasons.add(reason);
        }
        return reasons;
    }

    public static String normalizeReason(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    public String newReason(String output) {
        List<String> reasons = extractReasons(output);
        int presentReasonCount = reasons.size();
        if (presentReasonCount <= firedReasonCount) {
            firedReasonCount = presentReasonCount;
            return null;
        }
        firedReasonCount = presentReasonCount;
        return reasons.get(presentReasonCount - 1);
    }

    public void markTriggered(String reason) {
        // Retained for source compatibility. Per-occurrence deduplication is
        // performed by newReason tracking how many complete tags have already
        // fired, so an already-fired tag still present in scrollback never
        // re-fires on a re-scan of the transcript.
    }
}
