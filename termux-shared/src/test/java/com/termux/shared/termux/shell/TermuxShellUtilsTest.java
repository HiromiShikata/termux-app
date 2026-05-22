package com.termux.shared.termux.shell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TermuxShellUtilsTest {

    @Test
    public void setupShellCommandArgumentsMapsDataDataShebangToTermuxBinPrefix() throws IOException {
        File script = writeTempFile("login", "#!/data/data/com.termux.hs.shortcut/files/usr/bin/sh\necho hi\n");

        String[] result = TermuxShellUtils.setupShellCommandArguments(script.getAbsolutePath(), null);

        assertArrayEquals(new String[]{
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh",
            script.getAbsolutePath()
        }, result);
    }

    @Test
    public void setupShellCommandArgumentsMapsUsrShebangToTermuxBinPrefix() throws IOException {
        File script = writeTempFile("script", "#!/usr/bin/env\n");

        String[] result = TermuxShellUtils.setupShellCommandArguments(script.getAbsolutePath(), null);

        assertEquals(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/env", result[0]);
        assertEquals(script.getAbsolutePath(), result[1]);
    }

    @Test
    public void isSystemLinkerPathRecognizesBothLinkers() {
        assertTrue(TermuxShellUtils.isSystemLinkerPath(TermuxShellUtils.SYSTEM_LINKER_64_PATH));
        assertTrue(TermuxShellUtils.isSystemLinkerPath(TermuxShellUtils.SYSTEM_LINKER_PATH));
        assertFalse(TermuxShellUtils.isSystemLinkerPath(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh"));
    }

    @Test
    public void termuxAppDataDirConstantsDeriveFromForkPackageName() {
        assertEquals("/data/data/" + TermuxConstants.TERMUX_PACKAGE_NAME,
            TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);
        assertEquals("/data/user/0/" + TermuxConstants.TERMUX_PACKAGE_NAME,
            TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH_USER_0);
        assertFalse(TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH_USER_0
            .startsWith("/data/user/0/com.termux/"));
    }

    @Test
    public void isPathInsideTermuxAppDataDirRejectsPrefixOfDifferentPackage() {
        assertTrue(TermuxShellUtils.isPathInsideTermuxAppDataDir(
            TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files/usr/bin/sh"));
        assertFalse(TermuxShellUtils.isPathInsideTermuxAppDataDir(
            TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + ".other/files/usr/bin/sh"));
        assertFalse(TermuxShellUtils.isPathInsideTermuxAppDataDir("/data/data/com.termux/files/usr/bin/sh"));
    }

    @Test
    public void wrapWithSystemLinkerIfRequiredReturnsEmptyArrayUnchanged() {
        String[] empty = new String[0];

        assertArrayEquals(empty, TermuxShellUtils.wrapWithSystemLinkerIfRequired(empty));
    }

    @Test
    public void wrapWithSystemLinkerIfRequiredLeavesArgumentsUnchangedWhenExecNotRestricted() {
        String[] commandArguments = new String[]{
            TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files/usr/bin/login"
        };

        if (TermuxShellUtils.isAppDataFileExecRestricted()) {
            return;
        }

        assertArrayEquals(commandArguments,
            TermuxShellUtils.wrapWithSystemLinkerIfRequired(commandArguments));
    }

    @Test
    public void wrapWithSystemLinkerIfRequiredLeavesArgumentsOutsideAppDataDirUnchanged() {
        String[] commandArguments = new String[]{"/system/bin/sh"};

        assertArrayEquals(commandArguments,
            TermuxShellUtils.wrapWithSystemLinkerIfRequired(commandArguments));
    }

    private File writeTempFile(String name, String content) throws IOException {
        File file = File.createTempFile(name, null);
        file.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}
