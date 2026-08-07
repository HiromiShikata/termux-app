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
 * <p>Deduplication is therefore anchored to the monotonic sequence of DEDUPLICATION KEYS that have
 * streamed through, not to positions in the rendered string. Whitespace, line wrapping, and how much
 * of the front has scrolled away do not change that key sequence, so a still-visible tag is
 * recognized as already fired and does not re-fire. The keys of the occurrences visible in the
 * rendered window are a SUBSEQUENCE of the full fired-key stream: leading keys have scrolled off the
 * front, interior keys may have scrolled off so that occurrences still visible are no longer
 * contiguous in the stream, and trailing keys can also drop off the tail when the viewport is re-fed
 * from a fixed scrollback window (for example when the session-list bottom sheet is opened or the
 * session is switched). After the already-fired subsequence come the genuinely new occurrences
 * appended since the last scan.
 *
 * <p>The already-fired leading portion of the current window is found by walking both forward: each
 * current key is matched against the fired stream at or after a monotonically advancing cursor,
 * skipping over keys whose occurrences scrolled out of the window. The first current key that cannot
 * be located in the remaining fired stream begins the genuinely new suffix; everything from there on
 * is reported as new and appended to the fired stream. Advancing a cursor (rather than requiring the
 * fired TAIL to prefix the window) is what makes re-feeding the same transcript safe: when more than
 * one occurrence has fired and a re-fed window shows an older already-fired occurrence at the front
 * while the newest fired key is no longer a clean suffix anchor, the forward walk still recognizes
 * every visible occurrence as already fired, so the re-scan fires nothing while a genuinely new
 * appended occurrence still fires.
 */
public final class OutputTagScanner {

    private final OccurrenceExtractor occurrenceExtractor;

    /** The full ordered sequence of deduplication keys that have already fired, oldest first. This is
     * the monotonic stream prefix; it never shrinks even when the rendered window trims its front.
     *
     * <p>One session's transcript now reaches a single scanner instance from more than one thread —
     * the statusline parse thread walking the displayed sessions and the main thread feeding the
     * session the owner is viewing — so the read-then-append over this list in {@link
     * #newOccurrences(String)} MUST be atomic. Without that, two threads both observe an occurrence
     * as unfired and the owner is called twice for one request, or the concurrent append loses a key
     * and an already-answered call fires again. */
    private final List<String> firedDeduplicationKeys = new ArrayList<>();

    public interface ValueNormalizer {
        String normalize(String innerText);
    }

    public interface DeduplicationKeyResolver {
        String resolveDeduplicationKey(String value, String output, int occurrenceStartIndex);
    }

    public interface OccurrenceExtractor {
        List<OutputTagOccurrence> extractOccurrences(String output);
    }

    public OutputTagScanner(String tagName, ValueNormalizer valueNormalizer) {
        this(tagName, valueNormalizer, (value, output, occurrenceStartIndex) -> value);
    }

    public OutputTagScanner(String tagName, ValueNormalizer valueNormalizer,
                            DeduplicationKeyResolver deduplicationKeyResolver) {
        this(new TagBlockOccurrenceExtractor(tagName, valueNormalizer, deduplicationKeyResolver));
    }

    public OutputTagScanner(OccurrenceExtractor occurrenceExtractor) {
        this.occurrenceExtractor = occurrenceExtractor;
    }

    public List<String> extractValues(String output) {
        List<String> values = new ArrayList<>();
        for (OutputTagOccurrence occurrence : occurrenceExtractor.extractOccurrences(output)) {
            values.add(occurrence.getValue());
        }
        return values;
    }

    private static final class TagBlockOccurrenceExtractor implements OccurrenceExtractor {

        private final Pattern blockPattern;

        private final ValueNormalizer valueNormalizer;

        private final DeduplicationKeyResolver deduplicationKeyResolver;

        TagBlockOccurrenceExtractor(String tagName, ValueNormalizer valueNormalizer,
                                    DeduplicationKeyResolver deduplicationKeyResolver) {
            this.blockPattern = Pattern.compile("<" + tagName + ">([\\s\\S]*?)</" + tagName + ">");
            this.valueNormalizer = valueNormalizer;
            this.deduplicationKeyResolver = deduplicationKeyResolver;
        }

        @Override
        public List<OutputTagOccurrence> extractOccurrences(String output) {
            List<OutputTagOccurrence> occurrences = new ArrayList<>();
            if (output == null) return occurrences;

            Matcher matcher = blockPattern.matcher(output);
            while (matcher.find()) {
                String value = valueNormalizer.normalize(matcher.group(1));
                if (value == null) continue;
                occurrences.add(new OutputTagOccurrence(value,
                    deduplicationKeyResolver.resolveDeduplicationKey(value, output, matcher.start())));
            }
            return occurrences;
        }
    }

    public synchronized List<OutputTagOccurrence> newOccurrences(String currentText) {
        if (currentText == null) {
            return new ArrayList<>();
        }

        List<OutputTagOccurrence> currentOccurrences = occurrenceExtractor.extractOccurrences(currentText);

        int alreadyFiredInWindow = alreadyFiredPrefixLength(currentOccurrences);

        List<OutputTagOccurrence> newOccurrences = new ArrayList<>(
            currentOccurrences.subList(alreadyFiredInWindow, currentOccurrences.size()));
        for (OutputTagOccurrence occurrence : newOccurrences) {
            firedDeduplicationKeys.add(occurrence.getDeduplicationKey());
        }
        return newOccurrences;
    }

    public List<String> newValues(String currentText) {
        List<String> newValues = new ArrayList<>();
        for (OutputTagOccurrence occurrence : newOccurrences(currentText)) {
            newValues.add(occurrence.getValue());
        }
        return newValues;
    }

    /**
     * Returns the length of the leading portion of {@code currentOccurrences} that has already fired.
     * The visible occurrences' keys are a subsequence of {@link #firedDeduplicationKeys}: a forward
     * cursor over the fired stream matches each current key at or after the last match, skipping
     * keys whose occurrences scrolled out of the rendered window. The first current key that cannot
     * be located in the remaining fired stream begins the genuinely new suffix, so every occurrence
     * from that index on is new.
     */
    private int alreadyFiredPrefixLength(List<OutputTagOccurrence> currentOccurrences) {
        int firedCursor = 0;
        int currentIndex = 0;
        for (; currentIndex < currentOccurrences.size(); currentIndex++) {
            int matchIndex = indexOfKeyFrom(
                currentOccurrences.get(currentIndex).getDeduplicationKey(), firedCursor);
            if (matchIndex < 0) {
                break;
            }
            firedCursor = matchIndex + 1;
        }
        return currentIndex;
    }

    private int indexOfKeyFrom(String deduplicationKey, int firedStart) {
        for (int firedIndex = firedStart; firedIndex < firedDeduplicationKeys.size(); firedIndex++) {
            if (firedDeduplicationKeys.get(firedIndex).equals(deduplicationKey)) {
                return firedIndex;
            }
        }
        return -1;
    }
}
