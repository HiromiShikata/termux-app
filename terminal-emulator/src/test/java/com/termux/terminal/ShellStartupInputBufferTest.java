package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ShellStartupInputBufferTest {

    @Test
    public void heldInputIsHandedOverInTheOrderItWasSubmitted() {
        ShellStartupInputBuffer buffer = new ShellStartupInputBuffer();
        hold(buffer, "first ");
        hold(buffer, "second");

        Assert.assertEquals("first second", new String(buffer.drain(), StandardCharsets.UTF_8));
    }

    @Test
    public void drainingHandsOverEachByteOnlyOnce() {
        ShellStartupInputBuffer buffer = new ShellStartupInputBuffer();
        hold(buffer, "only once");
        buffer.drain();

        Assert.assertEquals(0, buffer.drain().length);
        Assert.assertEquals(0, buffer.heldByteCount());
    }

    @Test
    public void onlyTheRequestedRangeOfTheSubmittedArrayIsHeld() {
        ShellStartupInputBuffer buffer = new ShellStartupInputBuffer();
        byte[] data = "ignored-kept-ignored".getBytes(StandardCharsets.UTF_8);

        Assert.assertTrue(buffer.hold(data, "ignored-".length(), "kept".length()));
        Assert.assertEquals("kept", new String(buffer.drain(), StandardCharsets.UTF_8));
    }

    @Test
    public void inputBeyondTheCapacityIsRefusedSoItIsReportedAsDiscardedInstead() {
        ShellStartupInputBuffer buffer = new ShellStartupInputBuffer();
        byte[] full = new byte[ShellStartupInputBuffer.CAPACITY_BYTES];

        Assert.assertTrue(buffer.hold(full, 0, full.length));
        Assert.assertFalse(buffer.hold(new byte[]{'x'}, 0, 1));
        Assert.assertEquals(ShellStartupInputBuffer.CAPACITY_BYTES, buffer.heldByteCount());
    }

    @Test
    public void anEmptySubmissionIsNotHeld() {
        ShellStartupInputBuffer buffer = new ShellStartupInputBuffer();

        Assert.assertFalse(buffer.hold(new byte[]{'x'}, 0, 0));
        Assert.assertEquals(0, buffer.heldByteCount());
    }

    private static void hold(ShellStartupInputBuffer buffer, String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        Assert.assertTrue(buffer.hold(data, 0, data.length));
    }
}
