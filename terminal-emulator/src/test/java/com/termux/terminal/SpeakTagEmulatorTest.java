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
}
