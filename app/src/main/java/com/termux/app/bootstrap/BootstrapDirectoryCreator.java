package com.termux.app.bootstrap;

import java.io.File;
import java.io.IOException;

public interface BootstrapDirectoryCreator {

    void createBootstrapDirectory(File directory) throws IOException;
}
