package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class RemoteShellClientCommandTest {

    @Test
    public void aSessionStartedByAutosshIsRunByARemoteShellClient() {
        Assert.assertTrue(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/sh",
            new String[]{"-c", "autossh -M 0 -t user@host -p 9922 \"tmux new -D -A -s name\""}));
    }

    @Test
    public void aSessionThatInvokesSshDirectlyIsRunByARemoteShellClient() {
        Assert.assertTrue(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/ssh", new String[]{"user@host"}));
    }

    @Test
    public void aPlainLocalLoginShellIsNotRunByARemoteShellClient() {
        Assert.assertFalse(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/login", new String[]{}));
    }

    @Test
    public void aLocalShellWithNoArgumentsAtAllIsNotRunByARemoteShellClient() {
        Assert.assertFalse(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/bash", null));
    }

    @Test
    public void aProgramWhoseNameMerelyEndsInSshIsNotTheSshClient() {
        Assert.assertFalse(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/sshd", new String[]{"-D"}));
    }

    @Test
    public void aWordThatOnlyContainsSshInsideALongerNameIsNotTheSshClient() {
        Assert.assertFalse(RemoteShellClientCommand.isRunBy(
            "/data/data/com.termux/files/usr/bin/sh",
            new String[]{"-c", "myssherproject --run"}));
    }
}
