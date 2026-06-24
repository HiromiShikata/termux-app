package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TerminalInputEchoFilterTest {

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static int countGenuineOutput(TerminalInputEchoFilter filter, String output) {
        byte[] outputBytes = bytes(output);
        return filter.countGenuineOutput(outputBytes, 0, outputBytes.length);
    }

    private static void recordUserInput(TerminalInputEchoFilter filter, String input) {
        byte[] inputBytes = bytes(input);
        filter.recordUserInput(inputBytes, 0, inputBytes.length);
    }

    @Test
    public void echoedKeystrokesAreNotCountedAsGenuineOutput() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "ls");

        Assert.assertEquals(0, countGenuineOutput(filter, "ls"));
        Assert.assertFalse(filter.hasPendingEcho());
    }

    @Test
    public void outputWithoutAnyPriorInputIsAllGenuine() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();

        Assert.assertEquals(5, countGenuineOutput(filter, "hello"));
    }

    @Test
    public void programOutputArrivingRightAfterEchoStillCountsAsGenuine() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "ls\n");

        Assert.assertEquals(bytes("file.txt\n").length, countGenuineOutput(filter, "ls\nfile.txt\n"));
    }

    @Test
    public void echoSplitAcrossSeparateReadsIsFullyConsumed() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "echo");

        Assert.assertEquals(0, countGenuineOutput(filter, "ec"));
        Assert.assertEquals(0, countGenuineOutput(filter, "ho"));
        Assert.assertFalse(filter.hasPendingEcho());
    }

    @Test
    public void genuineOutputAfterEchoSplitAcrossReadsIsCounted() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "ls\n");

        Assert.assertEquals(0, countGenuineOutput(filter, "ls"));
        Assert.assertEquals(bytes("out").length, countGenuineOutput(filter, "\nout"));
    }

    @Test
    public void aPartialEchoMismatchDiscardsRemainingPendingEchoAndCountsRest() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "ls\n");

        Assert.assertEquals(bytes("Xfile").length, countGenuineOutput(filter, "lXfile"));
        Assert.assertFalse(filter.hasPendingEcho());
    }

    @Test
    public void laterGenuineOutputIsCountedAfterAllEchoConsumed() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "a");
        Assert.assertEquals(0, countGenuineOutput(filter, "a"));

        Assert.assertEquals(bytes("done\n").length, countGenuineOutput(filter, "done\n"));
    }

    @Test
    public void multipleKeystrokesEnqueuedBeforeEchoArrivesAreAllConsumed() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        recordUserInput(filter, "a");
        recordUserInput(filter, "b");
        recordUserInput(filter, "c");

        Assert.assertEquals(0, countGenuineOutput(filter, "abc"));
        Assert.assertFalse(filter.hasPendingEcho());
    }

    @Test
    public void pendingEchoBytesAreCappedToAvoidUnboundedGrowth() {
        TerminalInputEchoFilter filter = new TerminalInputEchoFilter();
        StringBuilder hugeInput = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            hugeInput.append('x');
        }
        recordUserInput(filter, hugeInput.toString());

        Assert.assertTrue(filter.hasPendingEcho());
    }
}
