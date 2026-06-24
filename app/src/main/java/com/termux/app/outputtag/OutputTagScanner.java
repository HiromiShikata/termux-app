package com.termux.app.outputtag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OutputTagScanner {

    public interface ValueNormalizer {
        String normalize(String innerText);
    }

    private final Pattern blockPattern;

    private final ValueNormalizer valueNormalizer;

    private String processedText = "";

    public OutputTagScanner(String tagName, ValueNormalizer valueNormalizer) {
        this.blockPattern = Pattern.compile("<" + tagName + ">([\\s\\S]*?)</" + tagName + ">");
        this.valueNormalizer = valueNormalizer;
    }

    public List<String> extractValues(String output) {
        List<String> values = new ArrayList<>();
        if (output == null) return values;

        Matcher matcher = blockPattern.matcher(output);
        while (matcher.find()) {
            String value = valueNormalizer.normalize(matcher.group(1));
            if (value != null) values.add(value);
        }
        return values;
    }

    public List<String> newValues(String currentText) {
        if (currentText == null) {
            processedText = "";
            return new ArrayList<>();
        }

        int alreadyProcessedBoundary = longestProcessedSuffixThatPrefixesCurrent(currentText);

        List<String> newValues = new ArrayList<>();
        Matcher matcher = blockPattern.matcher(currentText);
        while (matcher.find()) {
            if (matcher.end() <= alreadyProcessedBoundary) continue;
            String value = valueNormalizer.normalize(matcher.group(1));
            if (value != null) newValues.add(value);
        }

        processedText = currentText;
        return newValues;
    }

    private int longestProcessedSuffixThatPrefixesCurrent(String currentText) {
        int maxOverlap = Math.min(processedText.length(), currentText.length());
        for (int overlap = maxOverlap; overlap > 0; overlap--) {
            if (currentText.regionMatches(0, processedText, processedText.length() - overlap, overlap)) {
                return overlap;
            }
        }
        return 0;
    }
}
