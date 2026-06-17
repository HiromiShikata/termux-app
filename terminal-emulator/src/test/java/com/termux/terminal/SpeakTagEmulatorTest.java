package com.termux.terminal;

import java.util.Arrays;
import java.util.Collections;

public class SpeakTagEmulatorTest extends TerminalTestCase {

    public void testSpeakTagStrippedFromScreenAndTextSpoken() {
        withTerminalSized(20, 2)
            .enterString("<speak>hello world</speak>")
            .assertLinesAre("hello world         ", "                    ");
        assertEquals(Collections.singletonList("hello world"), mOutput.speakNotifications);
    }

    public void testSpeakTagSplitAcrossAppends() {
        withTerminalSized(20, 2);
        enterString("before <spea");
        enterString("k>hi</speak>!");
        assertLinesAre("before hi!          ", "                    ");
        assertEquals(Collections.singletonList("hi"), mOutput.speakNotifications);
    }

    public void testMultipleSpeakTagsSpokenInOrder() {
        withTerminalSized(20, 2)
            .enterString("<speak>one</speak> <speak>two</speak>")
            .assertLinesAre("one two             ", "                    ");
        assertEquals(Arrays.asList("one", "two"), mOutput.speakNotifications);
    }

    public void testTextWithoutSpeakTagIsUnchanged() {
        withTerminalSized(20, 2)
            .enterString("plain output")
            .assertLinesAre("plain output        ", "                    ");
        assertTrue(mOutput.speakNotifications.isEmpty());
    }

    public void testCloseMarkerSplitByEscapeSequenceIsStillDetected() {
        withTerminalSized(20, 2)
            .enterString("<speak>hi</spea\033[0mk> done")
            .assertLinesAre("hi done             ", "                    ");
        assertEquals(Collections.singletonList("hi"), mOutput.speakNotifications);
    }

    public void testInnerTextSplitByEscapeSequenceIsSpokenWithoutControlCodes() {
        withTerminalSized(20, 2)
            .enterString("<speak>hel\033[32mlo</speak>");
        assertEquals(Collections.singletonList("hello"), mOutput.speakNotifications);
    }

    public void testUnterminatedSpeakDoesNotReadEverything() {
        withTerminalSized(40, 30);
        StringBuilder runaway = new StringBuilder("<speak>");
        for (int i = 0; i < 600; i++) runaway.append('a');
        enterString(runaway.toString());
        assertTrue(mOutput.speakNotifications.isEmpty());

        enterString("<speak>ok</speak>");
        assertEquals(Collections.singletonList("ok"), mOutput.speakNotifications);
    }

    public void testStrayOpenMarkerBeforeWellFormedPairSpeaksOnlyLastInnerText() {
        withTerminalSized(40, 4)
            .enterString("<speak>stray runaway text <speak>hello</speak>");
        assertEquals(Collections.singletonList("hello"), mOutput.speakNotifications);
    }

    public void testScreenClearAbortsInProgressSpeak() {
        withTerminalSized(20, 4);
        enterString("<speak>runaway text");
        enterString("\033[2J");
        enterString("<speak>ok</speak>");
        assertEquals(Collections.singletonList("ok"), mOutput.speakNotifications);
    }

    public void testCursorHomeAbortsInProgressSpeak() {
        withTerminalSized(20, 4);
        enterString("<speak>runaway text");
        enterString("\033[H");
        enterString("<speak>ok</speak>");
        assertEquals(Collections.singletonList("ok"), mOutput.speakNotifications);
    }
}
