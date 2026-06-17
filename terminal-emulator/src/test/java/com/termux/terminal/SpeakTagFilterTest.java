package com.termux.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SpeakTagFilterTest {

    private static final class Capture {
        final StringBuilder display = new StringBuilder();
        final List<String> spoken = new ArrayList<>();
    }

    private static void feed(SpeakTagFilter filter, Capture capture, String chunk) {
        chunk.codePoints().forEach(codePoint ->
            filter.processCodePoint(codePoint, capture.display::appendCodePoint, capture.spoken::add));
    }

    @Test
    public void tagSplitAcrossTwoAppendsIsDetectedAndStripped() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "before <spea");
        feed(filter, capture, "k>world</speak>!");

        assertEquals("before world!", capture.display.toString());
        assertEquals(Collections.singletonList("world"), capture.spoken);
    }

    @Test
    public void closeMarkerSplitAcrossAppendsIsDetected() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "<speak>hello wor");
        feed(filter, capture, "ld</spe");
        feed(filter, capture, "ak> done");

        assertEquals("hello world done", capture.display.toString());
        assertEquals(Collections.singletonList("hello world"), capture.spoken);
    }

    @Test
    public void multipleTagsInOneStreamAreReadInOrder() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "<speak>one</speak> and <speak>two</speak> done");

        assertEquals("one and two done", capture.display.toString());
        assertEquals(Arrays.asList("one", "two"), capture.spoken);
    }

    @Test
    public void tagWithSurroundingTextKeepsInnerTextAndStripsMarkers() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "hello <speak>read me</speak> world");

        assertEquals("hello read me world", capture.display.toString());
        assertEquals(Collections.singletonList("read me"), capture.spoken);
    }

    @Test
    public void plainTextWithoutTagsIsPassedThroughUnchanged() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "plain text without tags");

        assertEquals("plain text without tags", capture.display.toString());
        assertTrue(capture.spoken.isEmpty());
    }

    @Test
    public void lessThanCharacterThatIsNotAMarkerIsDisplayed() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "a < b <speaker");

        assertEquals("a < b <speaker", capture.display.toString());
        assertTrue(capture.spoken.isEmpty());
    }

    @Test
    public void emptyTagDoesNotSpeakAndStripsMarkers() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "x<speak></speak>y");

        assertEquals("xy", capture.display.toString());
        assertTrue(capture.spoken.isEmpty());
    }

    @Test
    public void flushPendingReleasesHeldPartialMarkerToDisplay() {
        SpeakTagFilter filter = new SpeakTagFilter();
        Capture capture = new Capture();

        feed(filter, capture, "value <spea");
        assertTrue(filter.hasPending());
        filter.flushPending(capture.display::appendCodePoint);

        assertEquals("value <spea", capture.display.toString());
        assertTrue(capture.spoken.isEmpty());
    }
}
