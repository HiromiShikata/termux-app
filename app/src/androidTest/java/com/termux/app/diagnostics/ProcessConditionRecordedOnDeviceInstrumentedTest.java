package com.termux.app.diagnostics;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProcessConditionRecordedOnDeviceInstrumentedTest {

    private static final long RECORD_TIMEOUT_MILLIS = 10000L;

    private static final long RECORD_POLL_INTERVAL_MILLIS = 50L;

    @Test
    public void theConditionThisProcessRecordsLandsInTheFilesDirectoryOfTheRunningApp() {
        Context context = ApplicationProvider.getApplicationContext();
        long recordedAtMillis = System.currentTimeMillis();

        ProcessConditionSnapshotHolder.getInstance().recordCurrentCondition(
            ProcessConditionSnapshot.recorded(recordedAtMillis, 1000L, 12, 1, 82,
                recordedAtMillis - 1000L, 71, 3, ScrollAnswerTotals.NONE));

        ProcessConditionSnapshot readBack = awaitRecordWrittenSince(context, recordedAtMillis);

        assertTrue("the whole point of this record is that it outlives the process that wrote it,"
                + " so a build whose application never hands the holder a store, or whose store"
                + " cannot write into the private files directory of the running app, leaves every"
                + " later report saying no earlier process recorded its condition while the process"
                + " was in fact recording one. Read back instead: "
                + describe(readBack),
            readBack.isRecorded() && readBack.getRecordedAtMillis() >= recordedAtMillis);
    }

    private static ProcessConditionSnapshot awaitRecordWrittenSince(Context context,
                                                                    long recordedAtMillis) {
        long deadlineMillis = SystemClock.elapsedRealtime() + RECORD_TIMEOUT_MILLIS;
        ProcessConditionSnapshot readBack = ProcessConditionSnapshot.NOT_RECORDED;
        while (SystemClock.elapsedRealtime() < deadlineMillis) {
            readBack = ProcessConditionSnapshotFileStore
                .inFilesDirectory(context.getFilesDir()).read();
            if (readBack.isRecorded() && readBack.getRecordedAtMillis() >= recordedAtMillis) {
                return readBack;
            }
            SystemClock.sleep(RECORD_POLL_INTERVAL_MILLIS);
        }
        return readBack;
    }

    private static String describe(ProcessConditionSnapshot snapshot) {
        if (snapshot.isRecorded()) {
            return "a record written at " + snapshot.getRecordedAtMillis();
        }
        String unreadableReason = snapshot.getUnreadableReason();
        return unreadableReason == null ? "no record at all" : "an unreadable record: "
            + unreadableReason;
    }
}
