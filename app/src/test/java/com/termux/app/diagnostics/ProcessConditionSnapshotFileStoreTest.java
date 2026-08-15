package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ProcessConditionSnapshotFileStoreTest {

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private File recordFile() throws IOException {
        return new File(mTemporaryFolder.newFolder("files"), "process-condition-snapshot.txt");
    }

    @Test
    public void aConditionWrittenByOneProcessIsReadBackWholeByTheNext() throws IOException {
        File recordFile = recordFile();
        new ProcessConditionSnapshotFileStore(recordFile).write(
            ProcessConditionSnapshot.recorded(1786968180000L, 342000L, 12, 1, 82, 1786968102000L, 71, 3));

        ProcessConditionSnapshot readBack = new ProcessConditionSnapshotFileStore(recordFile).read();

        Assert.assertTrue("a record written and then read back as absent would leave the next process"
            + " believing no earlier process ever measured anything", readBack.isRecorded());
        Assert.assertEquals("the moment of the record places every other number in time",
            1786968180000L, readBack.getRecordedAtMillis());
        Assert.assertEquals("the uptime separates a process that degraded over hours from one that"
            + " failed at startup", 342000L, readBack.getProcessUptimeMillis());
        Assert.assertEquals("the queue depth at the last recorded moment is what a held main looper"
            + " shows", 12, readBack.getMainLooperPendingMessageCount());
        Assert.assertEquals("a synchronization barrier separates a blocked looper from a busy one",
            1, readBack.getSynchronizationBarrierCount());
        Assert.assertEquals("the deepest queue that process saw is the reading the investigation"
            + " turns on", 82, readBack.getPeakPendingMessageCount());
        Assert.assertEquals("the moment of that peak tells startup churn apart from a fault during"
            + " use", 1786968102000L, readBack.getPeakObservedAtMillis());
        Assert.assertEquals("the fade-capable view count at that depth separates leaked fade"
            + " callbacks from an ordinary queue", 71, readBack.getPeakScrollbarViewCount());
        Assert.assertEquals("a scroll gesture the terminal never drew for is the symptom itself",
            3, readBack.getKeptScrollWithoutDrawEpisodeCount());
    }

    @Test
    public void aProcessWithNoEarlierRecordReadsAsNotRecordedRatherThanAsZeroes() throws IOException {
        ProcessConditionSnapshot readBack = new ProcessConditionSnapshotFileStore(recordFile()).read();

        Assert.assertFalse("a first run has no earlier process, and zeroes would be read as a"
            + " measured quiet process", readBack.isRecorded());
        Assert.assertNull("nothing failed to be read on a first run, so naming a failure would send"
            + " the reader after a fault that does not exist", readBack.getUnreadableReason());
    }

    @Test
    public void aRecordThatCannotBeParsedReportsWhyRatherThanReadingAsAbsent() throws IOException {
        File recordFile = recordFile();
        PrintWriter writer = new PrintWriter(recordFile, "UTF-8");
        writer.print("processConditionSnapshot v1 recordedAtMillis=1786968180000");
        writer.close();

        ProcessConditionSnapshot readBack = new ProcessConditionSnapshotFileStore(recordFile).read();

        Assert.assertFalse("a partial record must not be presentable as a measured condition",
            readBack.isRecorded());
        String reason = readBack.getUnreadableReason();
        Assert.assertNotNull("a record that exists but cannot be read is a fault that has to be"
            + " visible rather than silently absent", reason);
        Assert.assertTrue("the reason has to name what was missing so the writing side can be"
            + " corrected. Actual reason: " + reason, reason.contains("processUptimeMillis"));
    }

    @Test
    public void aWrittenRecordReplacesTheOneTheEarlierProcessLeft() throws IOException {
        File recordFile = recordFile();
        ProcessConditionSnapshotFileStore store = new ProcessConditionSnapshotFileStore(recordFile);
        store.write(ProcessConditionSnapshot.recorded(1786968180000L, 342000L, 12, 1, 82,
            1786968102000L, 71, 3));
        store.write(ProcessConditionSnapshot.recorded(1786968240000L, 402000L, 4, 0, 90,
            1786968220000L, 8, 5));

        ProcessConditionSnapshot readBack = new ProcessConditionSnapshotFileStore(recordFile).read();

        Assert.assertEquals("a reader that got the older of two records would report a condition the"
            + " process had already moved past", 1786968240000L, readBack.getRecordedAtMillis());
        Assert.assertEquals("the deepest queue is the running maximum, so the later record carries"
            + " it", 90, readBack.getPeakPendingMessageCount());
    }
}
