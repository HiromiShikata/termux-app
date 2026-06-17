package com.termux.shared.shell.command;

import com.termux.shared.shell.command.ExecutionCommand.ExecutionState;
import com.termux.shared.shell.command.ExecutionCommand.Runner;
import com.termux.shared.shell.command.ExecutionCommand.ShellCreateMode;

import org.junit.Assert;
import org.junit.Test;

public class ExecutionCommandEnumsTest {

    @Test
    public void executionStateExposesNameAndValue() {
        Assert.assertEquals("Pre-Execution", ExecutionState.PRE_EXECUTION.getName());
        Assert.assertEquals(0, ExecutionState.PRE_EXECUTION.getValue());
        Assert.assertEquals("Executing", ExecutionState.EXECUTING.getName());
        Assert.assertEquals(1, ExecutionState.EXECUTING.getValue());
        Assert.assertEquals("Executed", ExecutionState.EXECUTED.getName());
        Assert.assertEquals(2, ExecutionState.EXECUTED.getValue());
        Assert.assertEquals("Success", ExecutionState.SUCCESS.getName());
        Assert.assertEquals(3, ExecutionState.SUCCESS.getValue());
        Assert.assertEquals("Failed", ExecutionState.FAILED.getName());
        Assert.assertEquals(4, ExecutionState.FAILED.getValue());
    }

    @Test
    public void runnerExposesName() {
        Assert.assertEquals("terminal-session", Runner.TERMINAL_SESSION.getName());
        Assert.assertEquals("app-shell", Runner.APP_SHELL.getName());
    }

    @Test
    public void runnerEqualsRunnerMatchesNameOnly() {
        Assert.assertTrue(Runner.APP_SHELL.equalsRunner("app-shell"));
        Assert.assertFalse(Runner.APP_SHELL.equalsRunner("terminal-session"));
        Assert.assertFalse(Runner.APP_SHELL.equalsRunner(null));
    }

    @Test
    public void runnerOfResolvesKnownNameAndNullForUnknown() {
        Assert.assertSame(Runner.TERMINAL_SESSION, Runner.runnerOf("terminal-session"));
        Assert.assertSame(Runner.APP_SHELL, Runner.runnerOf("app-shell"));
        Assert.assertNull(Runner.runnerOf("does-not-exist"));
        Assert.assertNull(Runner.runnerOf((String) null));
    }

    @Test
    public void runnerOfWithDefaultFallsBackWhenUnknown() {
        Assert.assertSame(Runner.APP_SHELL, Runner.runnerOf("app-shell", Runner.TERMINAL_SESSION));
        Assert.assertSame(Runner.TERMINAL_SESSION, Runner.runnerOf("unknown", Runner.TERMINAL_SESSION));
        Assert.assertSame(Runner.TERMINAL_SESSION, Runner.runnerOf(null, Runner.TERMINAL_SESSION));
    }

    @Test
    public void shellCreateModeExposesModeAndEquals() {
        Assert.assertEquals("always", ShellCreateMode.ALWAYS.getMode());
        Assert.assertEquals("no-shell-with-name", ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        Assert.assertTrue(ShellCreateMode.ALWAYS.equalsMode("always"));
        Assert.assertFalse(ShellCreateMode.ALWAYS.equalsMode("no-shell-with-name"));
        Assert.assertFalse(ShellCreateMode.ALWAYS.equalsMode(null));
    }

    @Test
    public void shellCreateModeOfResolvesKnownModeAndNullForUnknown() {
        Assert.assertSame(ShellCreateMode.ALWAYS, ShellCreateMode.modeOf("always"));
        Assert.assertSame(ShellCreateMode.NO_SHELL_WITH_NAME, ShellCreateMode.modeOf("no-shell-with-name"));
        Assert.assertNull(ShellCreateMode.modeOf("unknown"));
        Assert.assertNull(ShellCreateMode.modeOf(null));
    }
}
