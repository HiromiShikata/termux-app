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
 * recognized as already fired and does not re-fire. The tags visible in the rendered window are a
 * SUBSEQUENCE of the full fired-value stream: leading values have scrolled off the front, interior
 * values may have scrolled off so that values still visible are no longer contiguous in the stream,
 * and trailing values can also drop off the tail when the viewport is re-fed from a fixed scrollback
 * window (for example when the session-list bottom sheet is opened or the session is switched). After
 * the already-fired subsequence come the genuinely new values appended since the last scan.
 *
 * <p>The already-fired leading portion of the current window is found by walking both forward: each
 * current value is matched against the fired stream at or after a monotonically advancing cursor,
 * skipping over interior values that scrolled out of the window. The first current value that cannot
 * be located in the remaining fired stream begins the genuinely new suffix; everything from there on
 * is reported as new and appended to the fired stream. Advancing a cursor (rather than requiring the
 * fired TAIL to prefix the window) is what makes re-feeding the same transcript safe: when more than
 * one value has fired and a re-fed window shows an older already-fired value at the front while the
 * newest fired value is no longer a clean suffix anchor, the forward walk still recognizes every
 * visible value as already fired, so the re-scan fires nothing while a genuinely new appended value
 * still fires. Because the cursor only moves forward, a repeated value matches a distinct earlier
 * occurrence, so a genuinely new occurrence of an already-seen value is still reported.
 */
public final class OutputTagScanner {

    private final ValueExtractor valueExtractor;

    /** The full ordered sequence of normalized values that have already fired, oldest first. This is
     * the monotonic stream prefix; it never shrinks even when the rendered window trims its front. */
    private final List<String> firedValues = new ArrayList<>();

    public interface ValueNormalizer {
        String normalize(String innerText);
    }

    public interface ValueExtractor {
        List<String> extractValues(String output);
    }

    public OutputTagScanner(String tagName, ValueNormalizer valueNormalizer) {
        this(new TagBlockValueExtractor(tagName, valueNormalizer));
    }

    public OutputTagScanner(ValueExtractor valueExtractor) {
        this.valueExtractor = valueExtractor;
    }

    public List<String> extractValues(String output) {
        return valueExtractor.extractValues(output);
    }

    private static final class TagBlockValueExtractor implements ValueExtractor {

        private final Pattern blockPattern;

        private final ValueNormalizer valueNormalizer;

        TagBlockValueExtractor(String tagName, ValueNormalizer valueNormalizer) {
            this.blockPattern = Pattern.compile("<" + tagName + ">([\\s\\S]*?)</" + tagName + ">");
            this.valueNormalizer = valueNormalizer;
        }

        @Override
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
    }

    public List<String> newValues(String currentText) {
        if (currentText == null) {
            return new ArrayList<>();
        }

        List<String> currentValues = extractValues(currentText);

        int alreadyFiredInWindow = alreadyFiredPrefixLength(currentValues);

        List<String> newValues = new ArrayList<>(
            currentValues.subList(alreadyFiredInWindow, currentValues.size()));
        firedValues.addAll(newValues);
        return newValues;
    }

    /**
     * Returns the length of the leading portion of {@code currentValues} that has already fired. The
     * visible tags are a subsequence of {@link #firedValues}: a forward cursor over the fired stream
     * matches each current value at or after the last match, skipping interior values that scrolled
     * out of the rendered window. The first current value that cannot be located in the remaining
     * fired stream begins the genuinely new suffix, so every value from that index on is new.
     */
    private int alreadyFiredPrefixLength(List<String> currentValues) {
        int firedCursor = 0;
        int currentIndex = 0;
        for (; currentIndex < currentValues.size(); currentIndex++) {
            int matchIndex = indexOfValueFrom(currentValues.get(currentIndex), firedCursor);
            if (matchIndex < 0) {
                break;
            }
            firedCursor = matchIndex + 1;
        }
        return currentIndex;
    }

    private int indexOfValueFrom(String value, int firedStart) {
        for (int firedIndex = firedStart; firedIndex < firedValues.size(); firedIndex++) {
            if (firedValues.get(firedIndex).equals(value)) {
                return firedIndex;
            }
        }
        return -1;
    }
}
