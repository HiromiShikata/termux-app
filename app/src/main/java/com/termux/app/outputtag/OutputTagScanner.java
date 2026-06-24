package com.termux.app.outputtag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects complete {@code <tag>value</tag>} blocks in a terminal transcript and reports each
 * occurrence exactly once, the first time that occurrence becomes visible.
 *
 * <p>The transcript handed in on each scan is the rendered text of a fixed-size sliding window over
 * the terminal's circular scrollback buffer (see {@code TerminalBuffer.getTranscriptText()}). Once
 * the scrollback is full, new lines overwrite the oldest rows, so the FRONT of the rendered string
 * is chopped while the tail grows. A column resize reflows the window and changes its leading
 * whitespace and line wrapping. Neither of these is a clean append, so deduplication MUST NOT rely
 * on the new transcript being a character-for-character extension (a suffix overlap) of the previous
 * one — that assumption collapses under front-trimming and reflow and causes already-fired tags that
 * are still visible in the window to re-fire on every subsequent scan, producing an endless loop.
 *
 * <p>Deduplication is therefore anchored to the monotonic sequence of normalized tag VALUES that
 * have streamed through, not to positions in the rendered string. Whitespace, line wrapping, and how
 * much of the front has scrolled away do not change that value sequence, so a still-visible tag is
 * recognized as already fired and does not re-fire. The rendered window always contains a contiguous
 * tail-window of the full value stream (some leading values scrolled away, some new values appended
 * at the end); the longest already-fired tail that prefixes the current window marks the boundary,
 * and only the values beyond it are new.
 */
public final class OutputTagScanner {

    private final Pattern blockPattern;

    private final ValueNormalizer valueNormalizer;

    /** The full ordered sequence of normalized values that have already fired, oldest first. This is
     * the monotonic stream prefix; it never shrinks even when the rendered window trims its front. */
    private final List<String> firedValues = new ArrayList<>();

    public interface ValueNormalizer {
        String normalize(String innerText);
    }

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
            return new ArrayList<>();
        }

        List<String> currentValues = extractValues(currentText);

        int alreadyFiredInWindow = longestFiredSuffixThatPrefixesCurrent(currentValues);

        List<String> newValues = new ArrayList<>(
            currentValues.subList(alreadyFiredInWindow, currentValues.size()));
        firedValues.addAll(newValues);
        return newValues;
    }

    /**
     * Returns the length of the prefix of {@code currentValues} that has already fired. The rendered
     * window is a contiguous tail-window of the full value stream, so the already-fired portion still
     * visible is exactly the longest suffix of {@link #firedValues} that equals a prefix of
     * {@code currentValues}. Everything after that prefix is genuinely new.
     */
    private int longestFiredSuffixThatPrefixesCurrent(List<String> currentValues) {
        int maxOverlap = Math.min(firedValues.size(), currentValues.size());
        for (int overlap = maxOverlap; overlap > 0; overlap--) {
            if (firedSuffixEqualsCurrentPrefix(overlap, currentValues)) {
                return overlap;
            }
        }
        return 0;
    }

    private boolean firedSuffixEqualsCurrentPrefix(int overlap, List<String> currentValues) {
        int firedStart = firedValues.size() - overlap;
        for (int i = 0; i < overlap; i++) {
            if (!firedValues.get(firedStart + i).equals(currentValues.get(i))) {
                return false;
            }
        }
        return true;
    }
}
