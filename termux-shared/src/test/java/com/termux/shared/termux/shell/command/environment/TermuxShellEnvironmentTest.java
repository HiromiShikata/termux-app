package com.termux.shared.termux.shell.command.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TermuxShellEnvironmentTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void buildLdPreloadValueReturnsNullWhenNeitherLibraryPresent() throws IOException {
        File libDir = temporaryFolder.newFolder("lib");

        assertNull(TermuxShellEnvironment.buildLdPreloadValue(libDir));
    }

    @Test
    public void buildLdPreloadValueReturnsForkEaccesShimWhenOnlyShimPresent() throws IOException {
        File libDir = temporaryFolder.newFolder("lib");
        File forkShim = createEmptyFile(libDir, TermuxShellEnvironment.LIBTERMUX_FORK_EACCES_SHIM_FILE_NAME);

        String value = TermuxShellEnvironment.buildLdPreloadValue(libDir);

        assertEquals(forkShim.getAbsolutePath(), value);
    }

    @Test
    public void buildLdPreloadValueReturnsTermuxExecAloneWhenOnlyExecPresent() throws IOException {
        File libDir = temporaryFolder.newFolder("lib");
        File termuxExec = createEmptyFile(libDir, TermuxShellEnvironment.LIBTERMUX_EXEC_LD_PRELOAD_FILE_NAME);

        String value = TermuxShellEnvironment.buildLdPreloadValue(libDir);

        assertEquals(termuxExec.getAbsolutePath(), value);
    }

    @Test
    public void buildLdPreloadValuePrependsForkEaccesShimBeforeTermuxExecWhenBothPresent() throws IOException {
        File libDir = temporaryFolder.newFolder("lib");
        File forkShim = createEmptyFile(libDir, TermuxShellEnvironment.LIBTERMUX_FORK_EACCES_SHIM_FILE_NAME);
        File termuxExec = createEmptyFile(libDir, TermuxShellEnvironment.LIBTERMUX_EXEC_LD_PRELOAD_FILE_NAME);

        String value = TermuxShellEnvironment.buildLdPreloadValue(libDir);

        assertEquals(forkShim.getAbsolutePath() + " " + termuxExec.getAbsolutePath(), value);
        assertTrue(value.indexOf(TermuxShellEnvironment.LIBTERMUX_FORK_EACCES_SHIM_FILE_NAME)
            < value.indexOf(TermuxShellEnvironment.LIBTERMUX_EXEC_LD_PRELOAD_FILE_NAME));
    }

    private static File createEmptyFile(File parent, String name) throws IOException {
        File file = new File(parent, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[]{0});
        }
        return file;
    }
}
