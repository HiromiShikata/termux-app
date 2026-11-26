package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.List;

public final class CallToUserOccurrenceAlignment {

    private CallToUserOccurrenceAlignment() {
    }

    public static int matchedPrefixLength(List<OutputTagOccurrence> previouslySeen,
                                          List<OutputTagOccurrence> visible) {
        int maxLength = Math.min(previouslySeen.size(), visible.size());
        for (int length = maxLength; length > 0; length--) {
            if (matchesSuffixToPrefix(previouslySeen, visible, length)) {
                return length;
            }
        }
        return 0;
    }

    private static boolean matchesSuffixToPrefix(List<OutputTagOccurrence> previouslySeen,
                                                 List<OutputTagOccurrence> visible, int length) {
        int suffixStart = previouslySeen.size() - length;
        for (int index = 0; index < length; index++) {
            if (!isSameCall(previouslySeen.get(suffixStart + index), visible.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSameCall(OutputTagOccurrence previous, OutputTagOccurrence current) {
        if (previous.getDeduplicationKey().equals(current.getDeduplicationKey())) {
            return true;
        }
        return isBare(current) && current.getDeduplicationKey().equals(current.getValue())
            && current.getValue().equals(previous.getValue());
    }

    private static boolean isBare(OutputTagOccurrence occurrence) {
        return occurrence.getDeduplicationKey().equals(occurrence.getValue());
    }
}
