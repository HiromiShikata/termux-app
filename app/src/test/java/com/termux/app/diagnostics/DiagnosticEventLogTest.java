package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DiagnosticEventLogTest {

    @Test
    public void tailReturnsMostRecentEntriesInOrder() {
        DiagnosticEventLog log = new DiagnosticEventLog(10);
        log.record(1L, DiagnosticEventType.SESSION_CREATED, "a");
        log.record(2L, DiagnosticEventType.SESSION_REMOVED, "b");
        log.record(3L, DiagnosticEventType.WAKE_LOCK_ACQUIRED, "c");

        List<DiagnosticEvent> tail = log.tail(2);

        Assert.assertEquals(2, tail.size());
        Assert.assertEquals("b", tail.get(0).getDetail());
        Assert.assertEquals("c", tail.get(1).getDetail());
    }

    @Test
    public void recordingBeyondCapacityDropsOldestEntries() {
        DiagnosticEventLog log = new DiagnosticEventLog(2);
        log.record(1L, DiagnosticEventType.SESSION_CREATED, "first");
        log.record(2L, DiagnosticEventType.SESSION_CREATED, "second");
        log.record(3L, DiagnosticEventType.SESSION_CREATED, "third");

        Assert.assertEquals(2, log.size());
        List<DiagnosticEvent> tail = log.tail(10);
        Assert.assertEquals(2, tail.size());
        Assert.assertEquals("second", tail.get(0).getDetail());
        Assert.assertEquals("third", tail.get(1).getDetail());
    }

    @Test
    public void tailLargerThanSizeReturnsAllEntries() {
        DiagnosticEventLog log = new DiagnosticEventLog(10);
        log.record(1L, DiagnosticEventType.SESSION_CREATED, "only");

        List<DiagnosticEvent> tail = log.tail(50);

        Assert.assertEquals(1, tail.size());
        Assert.assertEquals("only", tail.get(0).getDetail());
    }

    @Test
    public void tailOfZeroOrNegativeReturnsEmpty() {
        DiagnosticEventLog log = new DiagnosticEventLog(10);
        log.record(1L, DiagnosticEventType.SESSION_CREATED, "only");

        Assert.assertTrue(log.tail(0).isEmpty());
        Assert.assertTrue(log.tail(-3).isEmpty());
    }

    @Test
    public void emptyLogTailIsEmpty() {
        DiagnosticEventLog log = new DiagnosticEventLog(10);

        Assert.assertEquals(0, log.size());
        Assert.assertTrue(log.tail(5).isEmpty());
    }

    @Test
    public void maxEntriesBelowOneIsClampedToOne() {
        DiagnosticEventLog log = new DiagnosticEventLog(0);
        log.record(1L, DiagnosticEventType.SESSION_CREATED, "first");
        log.record(2L, DiagnosticEventType.SESSION_CREATED, "second");

        Assert.assertEquals(1, log.size());
        Assert.assertEquals("second", log.tail(1).get(0).getDetail());
    }

    @Test
    public void defaultMaxEntriesIsTwoHundred() {
        DiagnosticEventLog log = new DiagnosticEventLog();
        for (int index = 0; index < DiagnosticEventLog.DEFAULT_MAX_ENTRIES + 50; index++) {
            log.record(index, DiagnosticEventType.SESSION_CREATED, String.valueOf(index));
        }

        Assert.assertEquals(DiagnosticEventLog.DEFAULT_MAX_ENTRIES, log.size());
        List<DiagnosticEvent> tail = log.tail(DiagnosticEventLog.DEFAULT_MAX_ENTRIES);
        Assert.assertEquals("50", tail.get(0).getDetail());
    }
}
