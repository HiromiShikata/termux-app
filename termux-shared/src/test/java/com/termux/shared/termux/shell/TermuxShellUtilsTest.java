package com.termux.shared.termux.shell;

import android.os.Build;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TermuxShellUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String BIN = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;

    private File writeFile(String name, byte[] content) throws IOException {
        File file = temporaryFolder.newFile(name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content);
        }
        return file;
    }

    @Test
    public void elfFileIsExecutedDirectlyWithoutInterpreter() throws IOException {
        byte[] elf = new byte[]{0x7F, 'E', 'L', 'F', 0x01, 0x02};
        File file = writeFile("program.elf", elf);
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{file.getAbsolutePath()}, result);
    }

    @Test
    public void scriptWithoutShebangUsesStandardShellInterpreter() throws IOException {
        File file = writeFile("script.sh", "echo hello world\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{BIN + "/sh", file.getAbsolutePath()}, result);
    }

    @Test
    public void shebangWithUsrBinPathIsRewrittenToTermuxBinInterpreter() throws IOException {
        File file = writeFile("python.py", "#!/usr/bin/python\nprint(1)\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{BIN + "/python", file.getAbsolutePath()}, result);
    }

    @Test
    public void shebangWithBinPathIsRewrittenToTermuxBinInterpreter() throws IOException {
        File file = writeFile("runner", "#!/bin/sh\necho hi\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{BIN + "/sh", file.getAbsolutePath()}, result);
    }

    @Test
    public void shebangWithLeadingWhitespaceIsParsed() throws IOException {
        File file = writeFile("env-runner", "#! /usr/bin/env\necho hi\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{BIN + "/env", file.getAbsolutePath()}, result);
    }

    @Test
    public void shebangWithUnsupportedPathDoesNotAddInterpreter() throws IOException {
        File file = writeFile("system-runner", "#!/system/bin/sh\necho hi\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{file.getAbsolutePath()}, result);
    }

    @Test
    public void argumentsAreAppendedAfterExecutable() throws IOException {
        File file = writeFile("script-with-args.sh", "echo hello world\n".getBytes());
        String[] result = TermuxShellUtils.setupShellCommandArguments(
            file.getAbsolutePath(), new String[]{"-a", "value"});
        Assert.assertArrayEquals(
            new String[]{BIN + "/sh", file.getAbsolutePath(), "-a", "value"}, result);
    }

    @Test
    public void missingFileFallsBackToExecutableWithArguments() {
        String missing = temporaryFolder.getRoot().getAbsolutePath() + "/does-not-exist";
        String[] result = TermuxShellUtils.setupShellCommandArguments(missing, new String[]{"-x"});
        Assert.assertArrayEquals(new String[]{missing, "-x"}, result);
    }

    @Test
    public void tinyFileBelowHeaderThresholdAddsNoInterpreter() throws IOException {
        File file = writeFile("tiny", new byte[]{'a', 'b'});
        String[] result = TermuxShellUtils.setupShellCommandArguments(file.getAbsolutePath(), null);
        Assert.assertArrayEquals(new String[]{file.getAbsolutePath()}, result);
    }
}
