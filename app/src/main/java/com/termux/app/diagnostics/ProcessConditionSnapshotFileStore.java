package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Scanner;

public final class ProcessConditionSnapshotFileStore implements ProcessConditionSnapshotStore {

    private static final String PENDING_WRITE_SUFFIX = ".writing";

    private static final Charset RECORD_CHARSET = Charset.forName("UTF-8");

    @NonNull
    private final File mRecordFile;

    public ProcessConditionSnapshotFileStore(@NonNull File recordFile) {
        mRecordFile = recordFile;
    }

    @Override
    @NonNull
    public ProcessConditionSnapshot read() {
        if (!mRecordFile.exists()) return ProcessConditionSnapshot.NOT_RECORDED;
        String text;
        Scanner scanner = null;
        try {
            scanner = new Scanner(mRecordFile, RECORD_CHARSET.name()).useDelimiter("\\A");
            text = scanner.hasNext() ? scanner.next() : "";
        } catch (IOException readFailure) {
            return ProcessConditionSnapshot.unreadable(mRecordFile.getName() + " could not be read: "
                + readFailure.getMessage());
        } finally {
            if (scanner != null) scanner.close();
        }
        try {
            return ProcessConditionSnapshotFormat.parse(text);
        } catch (RuntimeException parseFailure) {
            return ProcessConditionSnapshot.unreadable(mRecordFile.getName()
                + " does not hold a readable record: " + parseFailure.getMessage());
        }
    }

    @Override
    public void write(@NonNull ProcessConditionSnapshot snapshot) {
        File pendingFile = new File(mRecordFile.getParentFile(),
            mRecordFile.getName() + PENDING_WRITE_SUFFIX);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(pendingFile), RECORD_CHARSET);
            writer.write(ProcessConditionSnapshotFormat.format(snapshot));
        } catch (IOException writeFailure) {
            throw new IllegalStateException("the condition of this process could not be written to "
                + pendingFile.getName() + ", so nothing about it will outlive the process",
                writeFailure);
        } finally {
            closeQuietlyReportingFailure(writer, pendingFile);
        }
        if (!pendingFile.renameTo(mRecordFile)) {
            throw new IllegalStateException("the written condition could not be moved into place as "
                + mRecordFile.getName() + ", so the next process would read an older record");
        }
    }

    private static void closeQuietlyReportingFailure(Writer writer, @NonNull File pendingFile) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException closeFailure) {
            throw new IllegalStateException("the condition written to " + pendingFile.getName()
                + " was not flushed to disk, so the record left behind is incomplete", closeFailure);
        }
    }
}
