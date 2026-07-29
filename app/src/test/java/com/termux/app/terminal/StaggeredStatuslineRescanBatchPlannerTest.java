package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StaggeredStatuslineRescanBatchPlannerTest {

    private static final int BATCH_SIZE = 4;

    private static final long BATCH_INTERVAL_MILLIS = 1000L;

    private List<String> sessionNames(int count) {
        List<String> sessionNames = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            sessionNames.add(String.format("session-%02d", index));
        }
        return sessionNames;
    }

    private List<StaggeredStatuslineRescanBatchPlanner.Batch> plan(int sessionCount) {
        return StaggeredStatuslineRescanBatchPlanner.plan(sessionNames(sessionCount), BATCH_SIZE,
            BATCH_INTERVAL_MILLIS);
    }

    @Test
    public void planNoBatchForAnEmptySessionSet() {
        assertEquals(List.of(), plan(0));
    }

    @Test
    public void keepsASetThatFitsInOneBatchDueImmediately() {
        List<StaggeredStatuslineRescanBatchPlanner.Batch> batches = plan(BATCH_SIZE);

        assertEquals(1, batches.size());
        assertEquals(0L, batches.get(0).getDelayMillis());
        assertEquals(new LinkedHashSet<>(sessionNames(BATCH_SIZE)), batches.get(0).getSessionNames());
    }

    @Test
    public void neverPutsMoreThanTheBatchSizeOfTranscriptReadsInOneMainThreadPass() {
        for (StaggeredStatuslineRescanBatchPlanner.Batch batch : plan(31)) {
            assertTrue("a forced rescan pass must never materialize more than " + BATCH_SIZE
                    + " transcripts uninterrupted, yet one batch carried "
                    + batch.getSessionNames().size(),
                batch.getSessionNames().size() <= BATCH_SIZE);
        }
    }

    @Test
    public void spacesEachSuccessiveBatchOneIntervalFurtherOut() {
        List<StaggeredStatuslineRescanBatchPlanner.Batch> batches = plan(10);

        List<Long> delaysMillis = new ArrayList<>();
        for (StaggeredStatuslineRescanBatchPlanner.Batch batch : batches) {
            delaysMillis.add(batch.getDelayMillis());
        }
        assertEquals(List.of(0L, BATCH_INTERVAL_MILLIS, 2L * BATCH_INTERVAL_MILLIS), delaysMillis);
    }

    @Test
    public void coversEverySessionExactlyOnceInTheGivenOrder() {
        List<String> allSessionNames = sessionNames(10);

        List<String> plannedSessionNames = new ArrayList<>();
        for (StaggeredStatuslineRescanBatchPlanner.Batch batch : plan(10)) {
            plannedSessionNames.addAll(batch.getSessionNames());
        }

        assertEquals(allSessionNames, plannedSessionNames);
    }

    @Test
    public void deduplicatesRepeatedSessionNamesWithinABatch() {
        Set<String> batchSessionNames = StaggeredStatuslineRescanBatchPlanner
            .plan(Arrays.asList("session-a", "session-a", "session-b"), BATCH_SIZE,
                BATCH_INTERVAL_MILLIS)
            .get(0).getSessionNames();

        assertEquals(new LinkedHashSet<>(List.of("session-a", "session-b")), batchSessionNames);
    }

    @Test
    public void rejectsANonPositiveBatchSizeInsteadOfSilentlyScanningEverythingAtOnce() {
        assertThrows(IllegalArgumentException.class,
            () -> StaggeredStatuslineRescanBatchPlanner.plan(sessionNames(3), 0,
                BATCH_INTERVAL_MILLIS));
    }
}
