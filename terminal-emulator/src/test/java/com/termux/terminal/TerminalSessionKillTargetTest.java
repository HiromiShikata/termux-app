package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TerminalSessionKillTargetTest {

    @Test
    public void doesNotKillAnyProcessGroupForAnUnstartedShellPid() {
        Assert.assertEquals(TerminalSession.NO_SHELL_PROCESS_GROUP_TARGET,
            TerminalSession.shellProcessGroupKillTarget(0));
    }

    @Test
    public void doesNotKillAnyProcessGroupForTheInitPid() {
        Assert.assertEquals(TerminalSession.NO_SHELL_PROCESS_GROUP_TARGET,
            TerminalSession.shellProcessGroupKillTarget(1));
    }

    @Test
    public void doesNotKillAnyProcessGroupForAFinishedShellPid() {
        Assert.assertEquals(TerminalSession.NO_SHELL_PROCESS_GROUP_TARGET,
            TerminalSession.shellProcessGroupKillTarget(-1));
    }

    @Test
    public void killsTheNegatedProcessGroupForAStartedShellPid() {
        Assert.assertEquals(-4321, TerminalSession.shellProcessGroupKillTarget(4321));
    }

    @Test
    public void killsTheNegatedProcessGroupForTheSmallestRealShellPid() {
        Assert.assertEquals(-2, TerminalSession.shellProcessGroupKillTarget(2));
    }
}
