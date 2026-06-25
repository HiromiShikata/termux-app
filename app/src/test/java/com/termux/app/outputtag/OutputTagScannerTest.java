package com.termux.app.outputtag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class OutputTagScannerTest {

    private static OutputTagScanner trimNormalizingScanner() {
        return new OutputTagScanner("tag", OutputTagScannerTest::trimToNull);
    }

    private static String trimToNull(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Test
    public void firstScanReturnsEveryCompleteOccurrenceInOrder() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a", "b"),
            scanner.newValues("<tag>a</tag> mid <tag>b</tag>"));
    }

    @Test
    public void reScanningTheExactSameTranscriptDoesNotReFire() {
        OutputTagScanner scanner = trimNormalizingScanner();
        String transcript = "<tag>a</tag><tag>b</tag>";
        assertEquals(List.of("a", "b"), scanner.newValues(transcript));
        assertTrue(scanner.newValues(transcript).isEmpty());
    }

    @Test
    public void appendingPlainOutputAfterAFiredTagDoesNotReFireThatTag() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        assertTrue(scanner.newValues("<tag>a</tag>\nline 1").isEmpty());
        assertTrue(scanner.newValues("<tag>a</tag>\nline 1\nline 2").isEmpty());
    }

    @Test
    public void firesEachOfTwoNewTagsAppearingInOneUpdateExactlyOnce() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        assertEquals(List.of("b", "c"),
            scanner.newValues("<tag>a</tag><tag>b</tag><tag>c</tag>"));
    }

    @Test
    public void firesATagWhoseOpeningArrivedBeforeAndClosingArrivesAfterTheBoundary() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertTrue(scanner.newValues("prefix output <tag>partial").isEmpty());
        assertEquals(List.of("partial closed now"),
            scanner.newValues("prefix output <tag>partial closed now</tag>"));
    }

    @Test
    public void firesANewTagAfterEarlierContentScrolledOutOfTheTranscript() {
        OutputTagScanner scanner = trimNormalizingScanner();

        StringBuilder transcript = new StringBuilder("<tag>first</tag>\n");
        for (int line = 0; line < 4000; line++) {
            transcript.append("line ").append(line).append('\n');
        }
        assertEquals(List.of("first"), scanner.newValues(transcript.toString()));

        String trimmed = "line 3998\nline 3999\n<tag>second</tag>\n";
        assertEquals(List.of("second"), scanner.newValues(trimmed));
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagAfterTheTranscriptTrimmedItAway() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("first"),
            scanner.newValues("<tag>first</tag>\nline a\nline b\n"));
        assertTrue(scanner.newValues("line a\nline b\nplain continuation\n").isEmpty());
    }

    @Test
    public void treatsNullTranscriptAsNoNewValuesWithoutForgettingFiredTags() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        // A transient null transcript must not reset dedup state, otherwise the same tag would
        // re-fire on the next non-null scan that still shows it.
        assertTrue(scanner.newValues(null).isEmpty());
        assertTrue(scanner.newValues("<tag>a</tag>").isEmpty());
    }

    // --- Transcript-trimming and reflow regressions (the endless re-fire loop) ---

    @Test
    public void doesNotReFireAnAlreadyFiredTagThatIsStillVisibleAfterTheFrontWasTrimmed() {
        OutputTagScanner scanner = trimNormalizingScanner();

        // The transcript window is full: leading filler precedes a fired tag, trailing filler follows.
        assertEquals(List.of("keep"),
            scanner.newValues("X0\nX1\nX2\nX3\n<tag>keep</tag>\nA\nB\nC\n"));

        // The window slides forward: the oldest lines (X0, X1) are overwritten and a clean-append
        // suffix no longer exists, but the tag is still visible. It MUST NOT re-fire.
        assertTrue(scanner.newValues("X2\nX3\n<tag>keep</tag>\nA\nB\nC\nD\nE\n").isEmpty());

        // It continues to not re-fire as the window keeps sliding while the tag remains visible.
        assertTrue(scanner.newValues("<tag>keep</tag>\nA\nB\nC\nD\nE\nF\nG\n").isEmpty());
    }

    @Test
    public void doesNotReFireAnAlreadyFiredVisibleTagAfterAColumnResizeReflow() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("keep"),
            scanner.newValues("aaaa\nbbbb\n<tag>keep</tag>\ncccc\ndddd\n"));

        // A column resize reflows the wrapping: the leading lines are rejoined differently and a
        // new line is appended, so the rendered text is not a clean extension of the previous one.
        // The still-visible tag MUST NOT re-fire because its value is unchanged.
        assertTrue(scanner.newValues("aaaabbbb\n<tag>keep</tag>\ncccc\ndddd\neeee\n").isEmpty());
    }

    @Test
    public void firesAGenuinelyNewTagThatAppearsWhileAnEarlierTagIsStillVisibleAfterTrimming() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("one"),
            scanner.newValues("p0\np1\n<tag>one</tag>\nq0\nq1\n"));

        // Front trimmed (p0 gone) and a brand-new tag appended at the tail while "one" stays visible.
        assertEquals(List.of("two"),
            scanner.newValues("p1\n<tag>one</tag>\nq0\nq1\n<tag>two</tag>\n"));

        // Re-scanning the same window fires nothing.
        assertTrue(scanner.newValues("p1\n<tag>one</tag>\nq0\nq1\n<tag>two</tag>\n").isEmpty());
    }

    @Test
    public void firesANewOccurrenceOfARepeatedValueWithoutReFiringTheEarlierOne() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("dup"), scanner.newValues("<tag>dup</tag>\nline\n"));
        // A second occurrence of the same value appears while the first is still visible: only the
        // new occurrence fires.
        assertEquals(List.of("dup"),
            scanner.newValues("<tag>dup</tag>\nline\n<tag>dup</tag>\n"));
        // Re-scan: nothing new.
        assertTrue(scanner.newValues("<tag>dup</tag>\nline\n<tag>dup</tag>\n").isEmpty());
    }

    @Test
    public void doesNotReFireAnEarlierValueWhenAReFedWindowShowsItWhileTheNewerValueScrolledOffTheTail() {
        OutputTagScanner scanner = trimNormalizingScanner();

        // Two distinct values fire over the session.
        assertEquals(List.of("first"), scanner.newValues("<tag>first</tag>\n"));
        assertEquals(List.of("second"),
            scanner.newValues("<tag>first</tag>\n<tag>second</tag>\n"));

        // The viewport is re-fed (bottom sheet opened / session switched) showing the older value at
        // the front while the newer value is no longer in the rendered window, so the fired tail no
        // longer prefixes the window. The older value MUST NOT re-fire.
        assertTrue(scanner.newValues("<tag>first</tag>\nplain output\n").isEmpty());
    }

    @Test
    public void stillFiresAGenuinelyNewValueAfterAReFedWindowShowedOnlyOlderFiredValues() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("first"), scanner.newValues("<tag>first</tag>\n"));
        assertEquals(List.of("second"),
            scanner.newValues("<tag>first</tag>\n<tag>second</tag>\n"));
        assertTrue(scanner.newValues("<tag>first</tag>\nplain output\n").isEmpty());

        // A genuinely new value appended later still fires once.
        assertEquals(List.of("third"),
            scanner.newValues("<tag>first</tag>\nplain output\n<tag>third</tag>\n"));
        assertTrue(scanner.newValues("<tag>first</tag>\nplain output\n<tag>third</tag>\n").isEmpty());
    }

    @Test
    public void firesANewValueShownRightAfterAnOlderValueWhenTheInteriorValueScrolledOut() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("first"), scanner.newValues("<tag>first</tag>\n"));
        assertEquals(List.of("middle"),
            scanner.newValues("<tag>first</tag>\n<tag>middle</tag>\n"));

        // The interior value scrolled out of the window, so the window shows the oldest value at the
        // front immediately followed by a genuinely new value. Only the new value fires; the older
        // value does not re-fire.
        assertEquals(List.of("newest"),
            scanner.newValues("<tag>first</tag>\n<tag>newest</tag>\n"));
        // Re-scanning that same window fires nothing.
        assertTrue(scanner.newValues("<tag>first</tag>\n<tag>newest</tag>\n").isEmpty());
    }

    @Test
    public void firesAGenuinelyNewOccurrenceOfARepeatedValueAfterAReFedWindowAndDoesNotReFireOnReScan() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("repeat"), scanner.newValues("<tag>repeat</tag>\n"));
        assertEquals(List.of("other"),
            scanner.newValues("<tag>repeat</tag>\n<tag>other</tag>\n"));
        assertTrue(scanner.newValues("<tag>repeat</tag>\nplain\n").isEmpty());

        // A second genuine occurrence of the repeated value appears later: it fires exactly once.
        assertEquals(List.of("repeat"),
            scanner.newValues("<tag>repeat</tag>\nplain\n<tag>repeat</tag>\n"));
        assertTrue(scanner.newValues("<tag>repeat</tag>\nplain\n<tag>repeat</tag>\n").isEmpty());
    }
}
