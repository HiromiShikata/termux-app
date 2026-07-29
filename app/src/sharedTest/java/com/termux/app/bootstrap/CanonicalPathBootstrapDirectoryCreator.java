package com.termux.app.bootstrap;

import java.io.File;
import java.io.IOException;

public final class CanonicalPathBootstrapDirectoryCreator implements BootstrapDirectoryCreator {

    @Override
    public void createBootstrapDirectory(File directory) throws IOException {
        if (!isSymbolicLink(directory)) {
            if (directory.isDirectory()) {
                return;
            }
            if (!directory.exists() && !isNamedInItsParentDirectory(directory)) {
                if (!directory.mkdirs() && !directory.isDirectory()) {
                    throw new IOException("Creating bootstrap directory at path \""
                        + directory.getAbsolutePath() + "\" failed");
                }
                return;
            }
        }
        throw new IOException("Non-directory file found at bootstrap directory path \""
            + directory.getAbsolutePath() + "\"");
    }

    private static boolean isSymbolicLink(File directory) throws IOException {
        File absoluteDirectory = directory.getAbsoluteFile();
        File parentDirectory = absoluteDirectory.getParentFile();
        if (parentDirectory == null) {
            return false;
        }
        File directoryUnderCanonicalParent = new File(parentDirectory.getCanonicalFile(), absoluteDirectory.getName());
        return !directoryUnderCanonicalParent.getCanonicalFile().equals(directoryUnderCanonicalParent);
    }

    private static boolean isNamedInItsParentDirectory(File directory) {
        File absoluteDirectory = directory.getAbsoluteFile();
        File parentDirectory = absoluteDirectory.getParentFile();
        if (parentDirectory == null) {
            return false;
        }
        String[] namesInParentDirectory = parentDirectory.list();
        if (namesInParentDirectory == null) {
            return false;
        }
        for (String nameInParentDirectory : namesInParentDirectory) {
            if (nameInParentDirectory.equals(absoluteDirectory.getName())) {
                return true;
            }
        }
        return false;
    }
}
