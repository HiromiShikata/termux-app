package com.termux.app.api.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class ReceivedFileDestination {

    private final ReceivedFileDestinationStatus status;
    private final File file;

    private ReceivedFileDestination(ReceivedFileDestinationStatus status, File file) {
        this.status = status;
        this.file = file;
    }

    public static ReceivedFileDestination resolve(File receiveDir, String requestedName) throws IOException {
        String fileName = getLastPathSegment(requestedName);

        if (fileName.isEmpty()) {
            return new ReceivedFileDestination(ReceivedFileDestinationStatus.EMPTY_NAME, null);
        }

        if (fileName.equals(".") || fileName.equals("..")) {
            return new ReceivedFileDestination(ReceivedFileDestinationStatus.CURRENT_OR_PARENT_DIRECTORY_NAME, null);
        }

        File file = new File(receiveDir, fileName);

        if (!isDirectlyInside(receiveDir, file)) {
            return new ReceivedFileDestination(ReceivedFileDestinationStatus.OUTSIDE_RECEIVE_DIRECTORY, null);
        }

        return new ReceivedFileDestination(ReceivedFileDestinationStatus.ACCEPTED, file);
    }

    public ReceivedFileDestinationStatus getStatus() {
        return status;
    }

    public File getFile() {
        return file;
    }

    public void writeContent(InputStream in) throws IOException {
        if (status != ReceivedFileDestinationStatus.ACCEPTED) {
            throw new IllegalStateException("Content can only be written to an accepted destination, status=" + status);
        }

        try (FileOutputStream f = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = in.read(buffer)) > 0) {
                f.write(buffer, 0, readBytes);
            }
        }
    }

    private static String getLastPathSegment(String requestedName) {
        if (requestedName == null) return "";
        return requestedName.substring(requestedName.lastIndexOf('/') + 1);
    }

    private static boolean isDirectlyInside(File receiveDir, File file) throws IOException {
        if (Files.isSymbolicLink(file.toPath())) return false;
        return file.getCanonicalFile().getParentFile().equals(receiveDir.getCanonicalFile());
    }
}
