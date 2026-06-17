package com.termux.shared.shell;

import android.os.Build;

import com.termux.shared.logger.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class StreamGobblerTest {

    private static InputStream streamOf(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void writesEveryLineToTheProvidedList() {
        List<String> output = new ArrayList<>();
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf("alpha\nbeta\ngamma\n"), output, null);
        gobbler.run();
        Assert.assertEquals(3, output.size());
        Assert.assertEquals("alpha", output.get(0));
        Assert.assertEquals("beta", output.get(1));
        Assert.assertEquals("gamma", output.get(2));
    }

    @Test
    public void appendsEveryLineWithNewlineToTheProvidedStringBuilder() {
        StringBuilder output = new StringBuilder();
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf("one\ntwo\n"), output, null);
        gobbler.run();
        Assert.assertEquals("one\ntwo\n", output.toString());
    }

    @Test
    public void emptyStreamProducesNoOutput() {
        List<String> output = new ArrayList<>();
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf(""), output, null);
        gobbler.run();
        Assert.assertTrue(output.isEmpty());
    }

    @Test
    public void invokesLineListenerForEachLineAndStreamClosedListenerAtEnd() {
        List<String> seen = new ArrayList<>();
        AtomicBoolean closed = new AtomicBoolean(false);
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf("first\nsecond\n"),
            seen::add,
            () -> closed.set(true),
            null);
        gobbler.run();
        Assert.assertEquals(List.of("first", "second"), seen);
        Assert.assertTrue("stream closed listener must be invoked once the stream ends", closed.get());
    }

    @Test
    public void streamClosedListenerInvokedWhenReadThrowsIOException() {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream throwing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
        StreamGobbler gobbler = new StreamGobbler("sh", throwing, (StreamGobbler.OnLineListener) null,
            () -> closed.set(true), null);
        gobbler.run();
        Assert.assertTrue("stream closed listener must be invoked when reading fails", closed.get());
    }

    @Test
    public void runWithCustomLogLevelEnabledStillConsumesEntireStream() {
        Logger.setLogLevel(RuntimeEnvironment.getApplication(), Logger.LOG_LEVEL_VERBOSE);
        StringBuilder output = new StringBuilder();
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf("logged-line\n"), output, Logger.LOG_LEVEL_VERBOSE);
        gobbler.run();
        Assert.assertEquals("logged-line\n", output.toString());
    }

    @Test
    public void getInputStreamReturnsTheSourceStream() {
        InputStream source = streamOf("ignored\n");
        StreamGobbler gobbler = new StreamGobbler("sh", source, new ArrayList<>(), null);
        Assert.assertSame(source, gobbler.getInputStream());
    }

    @Test
    public void getOnLineListenerReturnsConfiguredListener() {
        StreamGobbler.OnLineListener listener = line -> { };
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf(""), listener, null, null);
        Assert.assertSame(listener, gobbler.getOnLineListener());
        StreamGobbler listWriterGobbler = new StreamGobbler("sh", streamOf(""), new ArrayList<>(), null);
        Assert.assertNull(listWriterGobbler.getOnLineListener());
    }

    @Test
    public void suspendAndResumeToggleSuspendedState() {
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf(""), new ArrayList<>(), null);
        Assert.assertFalse(gobbler.isSuspended());
        gobbler.suspendGobbling();
        Assert.assertTrue(gobbler.isSuspended());
        gobbler.waitForSuspend();
        Assert.assertTrue(gobbler.isSuspended());
        gobbler.resumeGobbling();
        Assert.assertFalse(gobbler.isSuspended());
    }

    @Test
    public void threadNameIsPrefixedWithGobbler() {
        StreamGobbler gobbler = new StreamGobbler("sh", streamOf(""), new ArrayList<>(), null);
        Assert.assertTrue(gobbler.getName().startsWith("Gobbler#"));
    }
}
