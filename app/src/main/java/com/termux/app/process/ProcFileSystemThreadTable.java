package com.termux.app.process;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class ProcFileSystemThreadTable implements ThreadTable {

    public static final String TASK_DIRECTORY_PATH = "/proc/self/task";

    private static final int SCHEDULER_STATE_FIELD_INDEX = 0;

    private static final int USER_TIME_FIELD_INDEX = 11;

    private static final int SYSTEM_TIME_FIELD_INDEX = 12;

    @NonNull
    private final File mTaskDirectory;

    public ProcFileSystemThreadTable() {
        this(new File(TASK_DIRECTORY_PATH));
    }

    public ProcFileSystemThreadTable(@NonNull File taskDirectory) {
        mTaskDirectory = taskDirectory;
    }

    @NonNull
    @Override
    public List<ProcessThread> threads() {
        String[] entries = mTaskDirectory.list();
        if (entries == null) {
            throw new IllegalStateException(
                "The thread table at " + mTaskDirectory.getPath() + " could not be listed");
        }
        List<ProcessThread> threads = new ArrayList<>();
        for (String entry : entries) {
            if (!isThreadIdentifier(entry)) {
                continue;
            }
            String statistics;
            try {
                statistics = readStatisticsOf(entry);
            } catch (IOException threadHasSinceExited) {
                continue;
            }
            threads.add(parseThread(entry, statistics));
        }
        return threads;
    }

    @NonNull
    private ProcessThread parseThread(@NonNull String threadIdentifier, @NonNull String statistics) {
        int nameOpeningIndex = statistics.indexOf('(');
        int nameClosingIndex = statistics.lastIndexOf(')');
        if (nameOpeningIndex < 0 || nameClosingIndex < nameOpeningIndex) {
            throw new IllegalStateException("The statistics of thread " + threadIdentifier
                + " carry no thread name in parentheses: " + statistics);
        }
        String name = statistics.substring(nameOpeningIndex + 1, nameClosingIndex);
        String[] fieldsAfterName = statistics.substring(nameClosingIndex + 1).trim().split("\\s+");
        if (fieldsAfterName.length <= SYSTEM_TIME_FIELD_INDEX) {
            throw new IllegalStateException("The statistics of thread " + threadIdentifier
                + " end before the processor time fields: " + statistics);
        }
        return new ProcessThread(threadIdentifier, name,
            fieldsAfterName[SCHEDULER_STATE_FIELD_INDEX],
            parseTicks(threadIdentifier, fieldsAfterName[USER_TIME_FIELD_INDEX]),
            parseTicks(threadIdentifier, fieldsAfterName[SYSTEM_TIME_FIELD_INDEX]));
    }

    private long parseTicks(@NonNull String threadIdentifier, @NonNull String field) {
        try {
            return Long.parseLong(field);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The processor time of thread " + threadIdentifier
                + " is not a number: " + field);
        }
    }

    @NonNull
    private String readStatisticsOf(@NonNull String threadIdentifier) throws IOException {
        File statisticsFile = new File(new File(mTaskDirectory, threadIdentifier), "stat");
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        try (InputStream input = new FileInputStream(statisticsFile)) {
            int readCount = input.read(buffer);
            while (readCount > 0) {
                collected.write(buffer, 0, readCount);
                readCount = input.read(buffer);
            }
        }
        return new String(collected.toByteArray(), Charset.forName("UTF-8"));
    }

    private boolean isThreadIdentifier(@NonNull String entry) {
        for (int index = 0; index < entry.length(); index++) {
            if (entry.charAt(index) < '0' || entry.charAt(index) > '9') {
                return false;
            }
        }
        return !entry.isEmpty();
    }
}
