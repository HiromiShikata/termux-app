package com.termux.app.process;

import androidx.annotation.NonNull;

import com.termux.app.diagnostics.DiagnosticsRenderThread;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RenderThreadReaderTest {

    private static final long CLOCK_TICKS_PER_SECOND = 100L;

    private static final class FixedThreadTable implements ThreadTable {

        @NonNull
        private final List<ProcessThread> mThreads;

        private FixedThreadTable(@NonNull List<ProcessThread> threads) {
            mThreads = threads;
        }

        @NonNull
        @Override
        public List<ProcessThread> threads() {
            return mThreads;
        }
    }

    private static final class UnreadableThreadTable implements ThreadTable {

        @NonNull
        @Override
        public List<ProcessThread> threads() {
            throw new IllegalStateException("the thread table could not be listed");
        }
    }

    private static DiagnosticsRenderThread readFrom(ThreadTable threadTable) {
        return new RenderThreadReader(threadTable, CLOCK_TICKS_PER_SECOND).read();
    }

    @Test
    public void theRenderThreadIsFoundByNameRatherThanByItsPositionAmongTheThreads() {
        DiagnosticsRenderThread renderThread = readFrom(new FixedThreadTable(Arrays.asList(
            new ProcessThread("2480", "main", "S", 100L, 10L),
            new ProcessThread("2481", "binder:2480_1", "S", 5L, 1L),
            new ProcessThread("2482", "RenderThread", "R", 19012L, 421L))));

        Assert.assertEquals("the render thread is not at a fixed position in the table, so selecting"
                + " by position would report whichever thread happened to be listed there",
            DiagnosticsRenderThread.Reading.MEASURED, renderThread.getReading());
        Assert.assertEquals("the state belongs to the render thread rather than to whichever thread"
            + " was listed first", "R", renderThread.getSchedulerState());
    }

    @Test
    public void processorTimeIsConvertedFromClockTicksSoTheReadingCanBeComparedWithTheStallMillis() {
        DiagnosticsRenderThread renderThread = readFrom(new FixedThreadTable(Arrays.asList(
            new ProcessThread("2482", "RenderThread", "R", 19012L, 421L))));

        Assert.assertEquals("the stalls in this reading are stated in milliseconds, so processor time"
            + " left in clock ticks cannot be compared against them", 190120L,
            renderThread.getUserTimeMillis());
        Assert.assertEquals("system time is converted on the same terms as user time", 4210L,
            renderThread.getSystemTimeMillis());
        Assert.assertEquals("what the caller waited on is the thread being busy at all, so the two"
            + " have to be available added together", 194330L, renderThread.getProcessorTimeMillis());
    }

    @Test
    public void aProcessRunningNoRenderThreadSaysSoRatherThanReportingNoProcessorTime() {
        DiagnosticsRenderThread renderThread = readFrom(new FixedThreadTable(Arrays.asList(
            new ProcessThread("2480", "main", "S", 100L, 10L))));

        Assert.assertEquals("a render thread that is genuinely absent is a different finding from one"
                + " that used no processor time, and reporting zero would state the second",
            DiagnosticsRenderThread.Reading.THREAD_ABSENT, renderThread.getReading());
    }

    @Test
    public void aThreadTableThatCannotBeReadIsReportedAsAFailureRatherThanAsAnIdleRenderThread() {
        DiagnosticsRenderThread renderThread = readFrom(new UnreadableThreadTable());

        Assert.assertEquals("a failure to measure reported as a measurement would have the reader rule"
                + " out a saturated render thread on the strength of a reading that never happened",
            DiagnosticsRenderThread.Reading.READ_FAILED, renderThread.getReading());
        Assert.assertTrue("the report has to state why the reading is missing. Actual: "
                + renderThread.getReadFailureMessage(),
            renderThread.getReadFailureMessage().contains("could not be listed"));
    }

    @Test
    public void askingAnUnmeasuredRenderThreadForItsProcessorTimeFailsRatherThanReturningZero() {
        DiagnosticsRenderThread renderThread = readFrom(new FixedThreadTable(Arrays.asList(
            new ProcessThread("2480", "main", "S", 100L, 10L))));

        try {
            renderThread.getProcessorTimeMillis();
            Assert.fail("a stand-in zero would be printed as a render thread that did no work, which is"
                + " the conclusion the reader is trying to test rather than one to hand them");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the failure has to say the thread was not measured. Actual: "
                + expected.getMessage(), expected.getMessage().contains("not measured"));
        }
    }

    @Test
    public void aClockTickRateThatCannotConvertProcessorTimeIsRejectedRatherThanUsedAsIs() {
        try {
            new RenderThreadReader(new FixedThreadTable(Arrays.asList(
                new ProcessThread("2482", "RenderThread", "R", 19012L, 421L))), 0L);
            Assert.fail("a zero tick rate would either divide by zero or be quietly replaced by a"
                + " guessed rate, and a guessed rate produces a processor time that looks measured");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue("the failure has to name the rate it was given. Actual: "
                + expected.getMessage(), expected.getMessage().contains("0"));
        }
    }
}
