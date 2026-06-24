package com.termux.app.terminal;

import com.termux.terminal.TerminalInputEchoFilter;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class SessionOutputEchoExclusionTest {

    private static final String SESSION_NAME = "worker";

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static final class GenuineOutputVersionModel {
        private final TerminalInputEchoFilter mEchoFilter = new TerminalInputEchoFilter();
        private long mGenuineOutputVersion = 0L;

        void typeUserInput(String input) {
            byte[] inputBytes = bytes(input);
            mEchoFilter.recordUserInput(inputBytes, 0, inputBytes.length);
        }

        void receivePtyOutput(String output) {
            byte[] outputBytes = bytes(output);
            int genuineBytes = mEchoFilter.countGenuineOutput(outputBytes, 0, outputBytes.length);
            if (genuineBytes > 0) mGenuineOutputVersion += genuineBytes;
        }

        long genuineOutputVersion() {
            return mGenuineOutputVersion;
        }
    }

    @Test
    public void typingThatIsEchoedBackDoesNotAdvanceOutRelativeTime() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        GenuineOutputVersionModel session = new GenuineOutputVersionModel();
        session.receivePtyOutput("ready\n");
        tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion());

        session.typeUserInput("l");
        session.receivePtyOutput("l");
        session.typeUserInput("s");
        session.receivePtyOutput("s");

        Assert.assertFalse(tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion()));
    }

    @Test
    public void genuineProgramOutputAdvancesOutRelativeTime() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        GenuineOutputVersionModel session = new GenuineOutputVersionModel();
        session.receivePtyOutput("ready\n");
        tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion());

        session.receivePtyOutput("build finished\n");

        Assert.assertTrue(tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion()));
    }

    @Test
    public void programOutputArrivingRightAfterTypingStillAdvancesOutRelativeTime() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        GenuineOutputVersionModel session = new GenuineOutputVersionModel();
        session.receivePtyOutput("ready\n");
        tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion());

        session.typeUserInput("ls\n");
        session.receivePtyOutput("ls\nfile.txt\n");

        Assert.assertTrue(tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion()));
    }

    @Test
    public void continuousTypingKeepsOutRelativeTimeFrozenAcrossManyKeystrokes() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        GenuineOutputVersionModel session = new GenuineOutputVersionModel();
        session.receivePtyOutput("ready\n");
        tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion());

        String typed = "the quick brown fox";
        for (int i = 0; i < typed.length(); i++) {
            String keystroke = typed.substring(i, i + 1);
            session.typeUserInput(keystroke);
            session.receivePtyOutput(keystroke);
            Assert.assertFalse(tracker.hasNewOutput(SESSION_NAME, session.genuineOutputVersion()));
        }
    }
}
