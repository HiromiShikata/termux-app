package com.termux.app.apkupdate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateTermuxUpTagScanner {

    private static final Pattern UPDATE_BLOCK_PATTERN =
        Pattern.compile("<update-termux-up>([\\s\\S]*?)</update-termux-up>");

    private String lastTriggeredReason;

    public static List<String> extractReasons(String output) {
        List<String> reasons = new ArrayList<>();
        if (output == null) return reasons;

        Matcher matcher = UPDATE_BLOCK_PATTERN.matcher(output);
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
        if (reasons.isEmpty()) return null;

        String latestReason = reasons.get(reasons.size() - 1);
        if (latestReason.equals(lastTriggeredReason)) return null;
        return latestReason;
    }

    public void markTriggered(String reason) {
        lastTriggeredReason = reason;
    }
}
