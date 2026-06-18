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

import java.io.File;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class AndroidShellEnvironmentTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getEnvironmentSetsFixedAndroidBaseVariables() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        HashMap<String, String> result = environment.getEnvironment(context(), false);
        Assert.assertEquals("/", result.get(UnixShellEnvironment.ENV_HOME));
        Assert.assertEquals("en_US.UTF-8", result.get(UnixShellEnvironment.ENV_LANG));
        Assert.assertEquals("/data/local/tmp", result.get(UnixShellEnvironment.ENV_TMPDIR));
        Assert.assertEquals("truecolor", result.get(UnixShellEnvironment.ENV_COLORTERM));
        Assert.assertEquals("xterm-256color", result.get(UnixShellEnvironment.ENV_TERM));
    }

    @Test
    public void getEnvironmentIsIndependentOfFailSafeFlag() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        HashMap<String, String> failSafe = environment.getEnvironment(context(), true);
        HashMap<String, String> normal = environment.getEnvironment(context(), false);
        Assert.assertEquals(normal, failSafe);
    }

    @Test
    public void getDefaultWorkingDirectoryPathIsRoot() {
        Assert.assertEquals("/", new AndroidShellEnvironment().getDefaultWorkingDirectoryPath());
    }

    @Test
    public void getDefaultBinPathIsSystemBin() {
        Assert.assertEquals("/system/bin", new AndroidShellEnvironment().getDefaultBinPath());
    }

    @Test
    public void setupShellCommandEnvironmentUsesAbsoluteWorkingDirectoryAsPwd() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        ExecutionCommand command = new ExecutionCommand();
        command.workingDirectory = "/data/local/tmp/./work/..";
        HashMap<String, String> result = environment.setupShellCommandEnvironment(context(), command);
        Assert.assertEquals(new File("/data/local/tmp/./work/..").getAbsolutePath(),
            result.get(UnixShellEnvironment.ENV_PWD));
    }

    @Test
    public void setupShellCommandEnvironmentFallsBackToDefaultWorkingDirectoryWhenUnset() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        ExecutionCommand command = new ExecutionCommand();
        command.workingDirectory = null;
        HashMap<String, String> result = environment.setupShellCommandEnvironment(context(), command);
        Assert.assertEquals("/", result.get(UnixShellEnvironment.ENV_PWD));
    }

    @Test
    public void setupShellCommandEnvironmentFallsBackToDefaultWorkingDirectoryWhenEmpty() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        ExecutionCommand command = new ExecutionCommand();
        command.workingDirectory = "";
        HashMap<String, String> result = environment.setupShellCommandEnvironment(context(), command);
        Assert.assertEquals("/", result.get(UnixShellEnvironment.ENV_PWD));
    }

    @Test
    public void setupShellCommandEnvironmentSkipsShellCommandVariablesWhenNotRequested() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(5);
        command.runner = ExecutionCommand.Runner.APP_SHELL.getName();
        command.setShellCommandShellEnvironment = false;
        HashMap<String, String> result = environment.setupShellCommandEnvironment(context(), command);
        Assert.assertFalse(result.containsKey(ShellCommandShellEnvironment.ENV_SHELL_CMD__RUNNER_NAME));
    }

    @Test
    public void setupShellCommandEnvironmentAddsShellCommandVariablesWhenRequested() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        ExecutionCommand command = new ExecutionCommand(9);
        command.runner = ExecutionCommand.Runner.APP_SHELL.getName();
        command.shellName = "demo-shell";
        command.setShellCommandShellEnvironment = true;
        HashMap<String, String> result = environment.setupShellCommandEnvironment(context(), command);
        Assert.assertEquals(ExecutionCommand.Runner.APP_SHELL.getName(),
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__RUNNER_NAME));
        Assert.assertEquals("demo-shell",
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__SHELL_NAME));
        Assert.assertEquals(context().getPackageName(),
            result.get(ShellCommandShellEnvironment.ENV_SHELL_CMD__PACKAGE_NAME));
    }

    @Test
    public void setupShellCommandArgumentsDelegatesToShellUtils() {
        AndroidShellEnvironment environment = new AndroidShellEnvironment();
        String[] arguments = environment.setupShellCommandArguments("/bin/sh", new String[]{"-c", "echo hi"});
        Assert.assertEquals("/bin/sh", arguments[0]);
        Assert.assertEquals("-c", arguments[1]);
        Assert.assertEquals("echo hi", arguments[2]);
    }
}
