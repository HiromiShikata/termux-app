package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TranscriptWorkCostCounterTest {

    private static final long ONE_MILLISECOND_IN_NANOS = 1000000L;

    @Test
    public void freshCounterReportsNoSamples() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        Assert.assertEquals("A counter that has never been recorded into must report zero samples, because the report"
                + " uses a zero count to state that the measured mechanism never ran",
            0L, counter.getSampleCount());
        Assert.assertEquals("Total elapsed time must be zero before any sample, so an unexercised mechanism cannot be"
                + " mistaken for a cheap one",
            0L, counter.getTotalElapsedMillis());
        Assert.assertEquals("The maximum must be zero before any sample, so the report can detect the no-sample case"
                + " from the count alone rather than from a sentinel duration",
            0L, counter.getMaxElapsedMillis());
        Assert.assertEquals("The transcript row count at the maximum must be zero before any sample, because no"
                + " transcript has been observed yet",
            0, counter.getTranscriptRowsAtMaxElapsed());
    }

    @Test
    public void samplesAccumulateIntoCountAndTotal() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        counter.record(3 * ONE_MILLISECOND_IN_NANOS, 100);
        counter.record(5 * ONE_MILLISECOND_IN_NANOS, 200);
        counter.record(7 * ONE_MILLISECOND_IN_NANOS, 300);

        Assert.assertEquals("Every recorded sample must be counted, because the report divides the total cost by the"
                + " count to reason about per-occurrence cost",
            3L, counter.getSampleCount());
        Assert.assertEquals("The total must be the sum of every sample, because it is the share of main-thread time"
                + " the measured mechanism has consumed over the process lifetime",
            15L, counter.getTotalElapsedMillis());
    }

    @Test
    public void maximumTracksTheLongestSample() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        counter.record(3 * ONE_MILLISECOND_IN_NANOS, 100);
        counter.record(41 * ONE_MILLISECOND_IN_NANOS, 4213);
        counter.record(5 * ONE_MILLISECOND_IN_NANOS, 200);

        Assert.assertEquals("The maximum must survive later shorter samples, because the user perceives the single"
                + " longest main-thread block, which an average or a last-value would hide",
            41L, counter.getMaxElapsedMillis());
    }

    @Test
    public void transcriptRowsAreRecordedFromTheSampleThatSetTheMaximum() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        counter.record(3 * ONE_MILLISECOND_IN_NANOS, 100);
        counter.record(41 * ONE_MILLISECOND_IN_NANOS, 4213);
        counter.record(5 * ONE_MILLISECOND_IN_NANOS, 9999);

        Assert.assertEquals("The row count must come from the sample that set the maximum, not from the latest sample,"
                + " because pairing the worst duration with the transcript size at that moment is what attributes the"
                + " cost to accumulated scrollback",
            4213, counter.getTranscriptRowsAtMaxElapsed());
    }

    @Test
    public void anEqualDurationDoesNotReplaceTheRecordedTranscriptRows() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        counter.record(41 * ONE_MILLISECOND_IN_NANOS, 4213);
        counter.record(41 * ONE_MILLISECOND_IN_NANOS, 12);

        Assert.assertEquals("A sample that merely ties the maximum must not overwrite the recorded row count, because"
                + " the first observation of that duration is the one already reasoned about and replacing it would"
                + " make the reported pair change without the reported maximum changing",
            4213, counter.getTranscriptRowsAtMaxElapsed());
    }

    @Test
    public void subMillisecondSamplesStillCountAndAccumulate() {
        TranscriptWorkCostCounter counter = new TranscriptWorkCostCounter();

        counter.record(600000L, 10);
        counter.record(600000L, 20);

        Assert.assertEquals("Samples shorter than a millisecond must still be counted, because the count is what tells"
                + " a reader how often the mechanism ran even while it is cheap",
            2L, counter.getSampleCount());
        Assert.assertEquals("Sub-millisecond samples must accumulate in nanoseconds before being reported in whole"
                + " milliseconds, otherwise many cheap occurrences would each truncate to zero and the accumulated"
                + " cost would never appear",
            1L, counter.getTotalElapsedMillis());
    }
}
