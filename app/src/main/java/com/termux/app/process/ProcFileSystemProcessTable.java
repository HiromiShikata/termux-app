package com.termux.app.process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class ProcFileSystemProcessTable implements ProcessTable {

    public static final String PROC_DIRECTORY_PATH = "/proc";

    private static final char COMMAND_LINE_ARGUMENT_SEPARATOR = '\0';

    @NonNull
    private final File mProcDirectory;

    public ProcFileSystemProcessTable() {
        this(new File(PROC_DIRECTORY_PATH));
    }

    public ProcFileSystemProcessTable(@NonNull File procDirectory) {
        mProcDirectory = procDirectory;
    }

    @NonNull
    @Override
    public List<String> processIdentifiers() {
        String[] entries = mProcDirectory.list();
        if (entries == null) {
            throw new IllegalStateException(
                "The process table at " + mProcDirectory.getPath() + " could not be listed");
        }
        List<String> identifiers = new ArrayList<>();
        for (String entry : entries) {
            if (isProcessIdentifier(entry)) {
                identifiers.add(entry);
            }
        }
        return identifiers;
    }

    @Nullable
    @Override
    public String commandNameOf(@NonNull String processIdentifier) {
        File commandLineFile = new File(new File(mProcDirectory, processIdentifier), "cmdline");
        byte[] commandLineBytes;
        try {
            commandLineBytes = readAllBytes(commandLineFile);
        } catch (IOException e) {
            return null;
        }
        String firstArgument = firstArgumentOf(new String(commandLineBytes, Charset.forName("UTF-8")));
        if (firstArgument.isEmpty()) {
            return null;
        }
        return baseNameOf(firstArgument);
    }

    @NonNull
    private byte[] readAllBytes(@NonNull File file) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        try (InputStream input = new FileInputStream(file)) {
            int readCount = input.read(buffer);
            while (readCount > 0) {
                collected.write(buffer, 0, readCount);
                readCount = input.read(buffer);
            }
        }
        return collected.toByteArray();
    }

    private boolean isProcessIdentifier(@NonNull String entry) {
        for (int index = 0; index < entry.length(); index++) {
            if (entry.charAt(index) < '0' || entry.charAt(index) > '9') {
                return false;
            }
        }
        return !entry.isEmpty();
    }

    @NonNull
    private String firstArgumentOf(@NonNull String commandLine) {
        int separatorIndex = commandLine.indexOf(COMMAND_LINE_ARGUMENT_SEPARATOR);
        return separatorIndex < 0 ? commandLine : commandLine.substring(0, separatorIndex);
    }

    @NonNull
    private String baseNameOf(@NonNull String executablePath) {
        String withoutLoginShellMarker =
            executablePath.startsWith("-") ? executablePath.substring(1) : executablePath;
        int lastSeparatorIndex = withoutLoginShellMarker.lastIndexOf('/');
        return lastSeparatorIndex < 0
            ? withoutLoginShellMarker
            : withoutLoginShellMarker.substring(lastSeparatorIndex + 1);
    }
}
