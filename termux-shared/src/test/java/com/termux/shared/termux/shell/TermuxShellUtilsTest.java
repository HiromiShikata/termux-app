package com.termux.shared.termux.shell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TermuxShellUtilsTest {

    @Test
    public void setupShellCommandArgumentsMapsUsrShebangToTermuxBinPrefix() throws IOException {
        File script = writeTempFile("script", "#!/usr/bin/env\n");

        String[] result = TermuxShellUtils.setupShellCommandArguments(script.getAbsolutePath(), null);

        assertEquals(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/env", result[0]);
        assertEquals(script.getAbsolutePath(), result[1]);
    }

    @Test
    public void setupShellCommandArgumentsMapsBinShebangToTermuxBinPrefix() throws IOException {
        File script = writeTempFile("script", "#!/bin/sh\n");

        String[] result = TermuxShellUtils.setupShellCommandArguments(script.getAbsolutePath(), null);

        assertArrayEquals(new String[]{
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh",
            script.getAbsolutePath()
        }, result);
    }

    @Test
    public void setupShellCommandArgumentsUsesStandardShellForFileWithoutShebangOrElf() throws IOException {
        File script = writeTempFile("script", "echo hi\n");

        String[] result = TermuxShellUtils.setupShellCommandArguments(script.getAbsolutePath(), null);

        assertArrayEquals(new String[]{
            TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh",
            script.getAbsolutePath()
        }, result);
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
