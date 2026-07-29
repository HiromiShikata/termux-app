package com.termux.app.bootstrap;

import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;

import java.io.File;
import java.io.IOException;

public final class FileUtilsBootstrapDirectoryCreator implements BootstrapDirectoryCreator {

    @Override
    public void createBootstrapDirectory(File directory) throws IOException {
        Error error = FileUtils.createDirectoryFile(directory.getAbsolutePath());
        if (error != null) {
            throw new IOException(Error.getMinimalErrorString(error));
        }
    }
}
