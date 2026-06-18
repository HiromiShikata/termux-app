package com.termux.shared.shell;

import org.junit.Assert;
import org.junit.Test;

public class ShellUtilsTest {

    @Test
    public void setupShellCommandArgumentsPutsExecutableFirstFollowedByArguments() {
        String[] result = ShellUtils.setupShellCommandArguments("/bin/sh", new String[]{"-c", "echo hi"});
        Assert.assertArrayEquals(new String[]{"/bin/sh", "-c", "echo hi"}, result);
    }

    @Test
    public void setupShellCommandArgumentsReturnsOnlyExecutableWhenArgumentsAreNull() {
        String[] result = ShellUtils.setupShellCommandArguments("/bin/sh", null);
        Assert.assertArrayEquals(new String[]{"/bin/sh"}, result);
    }

    @Test
    public void setupShellCommandArgumentsReturnsOnlyExecutableWhenArgumentsAreEmpty() {
        String[] result = ShellUtils.setupShellCommandArguments("/bin/sh", new String[]{});
        Assert.assertArrayEquals(new String[]{"/bin/sh"}, result);
    }

    @Test
    public void setupShellCommandArgumentsPreservesArgumentOrderAndDuplicates() {
        String[] result = ShellUtils.setupShellCommandArguments("cmd", new String[]{"a", "a", "b"});
        Assert.assertArrayEquals(new String[]{"cmd", "a", "a", "b"}, result);
    }

    @Test
    public void getExecutableBasenameReturnsFinalPathSegment() {
        Assert.assertEquals("bash", ShellUtils.getExecutableBasename("/usr/bin/bash"));
    }

    @Test
    public void getExecutableBasenameReturnsNullForNullExecutable() {
        Assert.assertNull(ShellUtils.getExecutableBasename(null));
    }

    @Test
    public void getPidReturnsNegativeOneWhenProcessHasNoAccessiblePidField() {
        Process processWithoutPidField = new Process() {
            @Override public java.io.OutputStream getOutputStream() { return null; }
            @Override public java.io.InputStream getInputStream() { return null; }
            @Override public java.io.InputStream getErrorStream() { return null; }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() { return 0; }
            @Override public void destroy() { }
        };
        Assert.assertEquals(-1, ShellUtils.getPid(processWithoutPidField));
    }

    @Test
    public void getTerminalSessionTranscriptTextReturnsNullForNullSession() {
        Assert.assertNull(ShellUtils.getTerminalSessionTranscriptText(null, false, false));
    }
}
