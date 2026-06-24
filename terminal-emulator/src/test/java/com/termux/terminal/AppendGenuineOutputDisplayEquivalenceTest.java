package com.termux.terminal;

import java.nio.charset.StandardCharsets;

public class AppendGenuineOutputDisplayEquivalenceTest extends TerminalTestCase {

    private String renderWholeAppend(String output) {
        withTerminalSized(40, 6);
        byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
        mTerminal.append(bytes, bytes.length);
        return mTerminal.getScreen().getTranscriptText();
    }

    private String renderSplitAppend(String output, int splitAt) {
        withTerminalSized(40, 6);
        byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
        if (splitAt > 0) mTerminal.append(bytes, splitAt);
        mTerminal.appendGenuineOutput(bytes, splitAt, bytes.length - splitAt);
        return mTerminal.getScreen().getTranscriptText();
    }

    public void testSplitAppendRendersIdenticalScreenForEverySplitPoint() {
        String output = "ls\r\nfile-one.txt\r\nfile-two.txt\r\n\033[1;1Hprompt$ ";
        String whole = renderWholeAppend(output);

        for (int splitAt = 0; splitAt <= output.getBytes(StandardCharsets.UTF_8).length; splitAt++) {
            assertEquals("the rendered screen must be identical regardless of where the echo/genuine split falls (splitAt=" + splitAt + ")",
                whole, renderSplitAppend(output, splitAt));
        }
    }

    public void testSplitAppendRendersIdenticalScreenForEscapeHeavyOutput() {
        String output = "\033[?1049h\033[2J\033[1;1Hcontent line one\033[2;1Hcontent line two\033[24;1Hstatus";
        String whole = renderWholeAppend(output);

        for (int splitAt = 0; splitAt <= output.getBytes(StandardCharsets.UTF_8).length; splitAt++) {
            assertEquals("escape-heavy output must render identically regardless of split point (splitAt=" + splitAt + ")",
                whole, renderSplitAppend(output, splitAt));
        }
    }
}
