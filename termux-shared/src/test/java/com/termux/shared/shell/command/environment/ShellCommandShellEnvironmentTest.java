package com.termux.shared.shell.command.environment;

import android.content.Context;
import android.os.Build;

import com.termux.shared.shell.command.ExecutionCommand;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ShellCommandShellEnvironmentTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getEnvironmentReturnsEmptyMapWhenRunnerUnrecognised() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(1);
        command.runner = "non-existent-runner";
        HashMap<String, String> result = environment.getEnvironment(context(), command);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getEnvironmentReturnsEmptyMapWhenRunnerNull() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(1);
        command.runner = null;
        Assert.assertTrue(environment.getEnvironment(context(), command).isEmpty());
    }

    @Test
    public void getEnvironmentPopulatesRunnerAndPackageAndShellId() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(42);
        command.runner = ExecutionCommand.Runner.TERMINAL_SESSION.getName();
        HashMap<String, String> result = environment.getEnvironment(context(), command);
        Assert.assertEquals(ExecutionCommand.Runner.TERMINAL_SESSION.getName(),
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__RUNNER_NAME));
        Assert.assertEquals(context().getPackageName(),
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__PACKAGE_NAME));
        Assert.assertEquals("42", result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__SHELL_ID));
    }

    @Test
    public void getEnvironmentOmitsShellNameWhenNotSet() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(7);
        command.runner = ExecutionCommand.Runner.APP_SHELL.getName();
        command.shellName = null;
        HashMap<String, String> result = environment.getEnvironment(context(), command);
        Assert.assertFalse(result.containsKey(ShellCommandShellEnvironment.ENV_SHELL_CMD__SHELL_NAME));
    }

    @Test
    public void getEnvironmentIncludesShellNameWhenSet() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(8);
        command.runner = ExecutionCommand.Runner.APP_SHELL.getName();
        command.shellName = "build-shell";
        HashMap<String, String> result = environment.getEnvironment(context(), command);
        Assert.assertEquals("build-shell",
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__SHELL_NAME));
    }

    @Test
    public void shellIdIsStringOfNullWhenIdNull() {
        ShellCommandShellEnvironment environment = new ShellCommandShellEnvironment();
        ExecutionCommand command = new ExecutionCommand();
        command.runner = ExecutionCommand.Runner.APP_SHELL.getName();
        HashMap<String, String> result = environment.getEnvironment(context(), command);
        Assert.assertEquals("null", result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__SHELL_ID));
    }
}
