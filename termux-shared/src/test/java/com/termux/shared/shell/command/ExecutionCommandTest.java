package com.termux.shared.shell.command;

import com.termux.shared.errors.Error;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand.ExecutionState;
import com.termux.shared.shell.command.ExecutionCommand.Runner;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;

public class ExecutionCommandTest {

    private static int originalLogLevel;

    @BeforeClass
    public static void silenceAndroidLog() throws Exception {
        Field field = Logger.class.getDeclaredField("CURRENT_LOG_LEVEL");
        field.setAccessible(true);
        originalLogLevel = field.getInt(null);
        field.setInt(null, Logger.LOG_LEVEL_OFF);
    }

    @AfterClass
    public static void restoreAndroidLog() throws Exception {
        Field field = Logger.class.getDeclaredField("CURRENT_LOG_LEVEL");
        field.setAccessible(true);
        field.setInt(null, originalLogLevel);
    }

    @Test
    public void fullConstructorRetainsAllProvidedValues() {
        ExecutionCommand command = new ExecutionCommand(7, "/bin/sh", new String[]{"-c", "echo hi"},
            "input", "/home", Runner.APP_SHELL.getName(), true);
        Assert.assertEquals(Integer.valueOf(7), command.id);
        Assert.assertEquals("/bin/sh", command.executable);
        Assert.assertEquals(2, command.arguments.length);
        Assert.assertEquals("input", command.stdin);
        Assert.assertEquals("/home", command.workingDirectory);
        Assert.assertEquals(Runner.APP_SHELL.getName(), command.runner);
        Assert.assertTrue(command.isFailsafe);
    }

    @Test
    public void idOnlyConstructorSetsIdAndDefaultState() {
        ExecutionCommand command = new ExecutionCommand(3);
        Assert.assertEquals(Integer.valueOf(3), command.id);
        Assert.assertFalse(command.hasExecuted());
        Assert.assertFalse(command.isExecuting());
        Assert.assertFalse(command.isSuccessful());
    }

    @Test
    public void setStateAdvancesForwardAndTracksExecutionFlags() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertTrue(command.setState(ExecutionState.EXECUTING));
        Assert.assertTrue(command.isExecuting());
        Assert.assertFalse(command.hasExecuted());
        Assert.assertTrue(command.setState(ExecutionState.EXECUTED));
        Assert.assertTrue(command.hasExecuted());
        Assert.assertTrue(command.setState(ExecutionState.SUCCESS));
        Assert.assertTrue(command.isSuccessful());
    }

    @Test
    public void setStateRejectsBackwardTransition() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertTrue(command.setState(ExecutionState.EXECUTED));
        Assert.assertFalse(command.setState(ExecutionState.EXECUTING));
    }

    @Test
    public void setStateCannotChangeAfterSuccess() {
        ExecutionCommand command = new ExecutionCommand();
        command.setState(ExecutionState.SUCCESS);
        Assert.assertFalse(command.setState(ExecutionState.FAILED));
        Assert.assertTrue(command.isSuccessful());
    }

    @Test
    public void setStateFailedFromErrorMarksFailedAndStateFailed() {
        ExecutionCommand command = new ExecutionCommand();
        Error error = new Error("boom-type", 100, "boom");
        Assert.assertTrue(command.setStateFailed(error));
        Assert.assertTrue(command.isStateFailed());
        Assert.assertFalse(command.isSuccessful());
    }

    @Test
    public void setStateFailedFromErrorWithThrowableVariants() {
        Error error = new Error(101, "boom");
        ExecutionCommand single = new ExecutionCommand();
        Assert.assertTrue(single.setStateFailed(error, new RuntimeException("x")));
        Assert.assertTrue(single.isStateFailed());

        ExecutionCommand list = new ExecutionCommand();
        Assert.assertTrue(list.setStateFailed(error, Collections.singletonList(new RuntimeException("y"))));
        Assert.assertTrue(list.isStateFailed());
    }

    @Test
    public void setStateFailedFromCodeAndMessageVariants() {
        ExecutionCommand plain = new ExecutionCommand();
        Assert.assertTrue(plain.setStateFailed(102, "plain"));
        Assert.assertTrue(plain.isStateFailed());

        ExecutionCommand withThrowable = new ExecutionCommand();
        Assert.assertTrue(withThrowable.setStateFailed(103, "throwable", new RuntimeException("z")));
        Assert.assertTrue(withThrowable.isStateFailed());

        ExecutionCommand withList = new ExecutionCommand();
        Assert.assertTrue(withList.setStateFailed(104, "list",
            Collections.singletonList(new RuntimeException("w"))));
        Assert.assertTrue(withList.isStateFailed());
    }

    @Test
    public void isStateFailedReturnsFalseWhenNotFailed() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertFalse(command.isStateFailed());
    }

    @Test
    public void shouldNotProcessResultsIsTrueOnlyAfterFirstCall() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertFalse(command.shouldNotProcessResults());
        Assert.assertTrue(command.shouldNotProcessResults());
    }

    @Test
    public void pluginExecutionCommandWithPendingResultRequiresBothConditions() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertFalse(command.isPluginExecutionCommandWithPendingResult());
        command.isPluginExecutionCommand = true;
        command.resultConfig.resultDirectoryPath = "/tmp/out";
        Assert.assertTrue(command.isPluginExecutionCommandWithPendingResult());
    }

    @Test
    public void toStringUsesInputLogBeforeExecutionAndOutputLogAfter() {
        ExecutionCommand command = new ExecutionCommand(1);
        command.commandLabel = "Demo";
        String beforeExecution = command.toString();
        Assert.assertTrue(beforeExecution.contains("Executable"));

        command.setState(ExecutionState.EXECUTED);
        String afterExecution = command.toString();
        Assert.assertTrue(afterExecution.contains("Current State"));
        Assert.assertFalse(afterExecution.contains("Executable"));
    }

    @Test
    public void getExecutionInputLogStringIncludesAppShellSpecificEntries() {
        ExecutionCommand command = new ExecutionCommand(5, "/bin/sh", new String[]{"-l"},
            "stdin-data", "/work", Runner.APP_SHELL.getName(), false);
        command.mPid = 4242;
        command.backgroundCustomLogLevel = Logger.LOG_LEVEL_VERBOSE;
        command.sessionAction = "0";
        command.shellName = "shell";
        command.shellCreateMode = ExecutionCommand.ShellCreateMode.ALWAYS.getMode();
        command.isPluginExecutionCommand = true;
        String logString = ExecutionCommand.getExecutionInputLogString(command, false, true);
        Assert.assertTrue(logString.contains("Pid"));
        Assert.assertTrue(logString.contains("Stdin"));
        Assert.assertTrue(logString.contains("Background Custom Log Level"));
        Assert.assertTrue(logString.contains("Session Action"));
        Assert.assertTrue(logString.contains("Shell Name"));
        Assert.assertTrue(logString.contains("Command Intent"));
    }

    @Test
    public void getExecutionInputLogStringHonoursIgnoreNull() {
        ExecutionCommand command = new ExecutionCommand(6, "/bin/sh", null, null, "/work",
            Runner.TERMINAL_SESSION.getName(), false);
        String ignoring = ExecutionCommand.getExecutionInputLogString(command, true, true);
        Assert.assertFalse(ignoring.contains("Session Action"));
        Assert.assertFalse(ignoring.contains("Command Intent"));
    }

    @Test
    public void getExecutionInputLogStringForNullReturnsNullLiteral() {
        Assert.assertEquals("null", ExecutionCommand.getExecutionInputLogString(null, true, true));
    }

    @Test
    public void getExecutionOutputAndDetailedLogStrings() {
        ExecutionCommand command = new ExecutionCommand(8);
        command.commandLabel = "Out";
        command.setState(ExecutionState.EXECUTED);
        String output = ExecutionCommand.getExecutionOutputLogString(command, true, true, true);
        Assert.assertTrue(output.contains("Current State"));
        Assert.assertEquals("null", ExecutionCommand.getExecutionOutputLogString(null, true, true, true));

        String detailed = ExecutionCommand.getDetailedLogString(command);
        Assert.assertTrue(detailed.contains("Command Description"));
        Assert.assertEquals("null", ExecutionCommand.getDetailedLogString(null));
    }

    @Test
    public void getExecutionCommandMarkdownStringRendersSections() {
        ExecutionCommand command = new ExecutionCommand(9, "/bin/sh", new String[]{"-c", "id"},
            "markdown-stdin", "/work", Runner.APP_SHELL.getName(), false);
        command.mPid = 99;
        command.backgroundCustomLogLevel = Logger.LOG_LEVEL_DEBUG;
        command.commandDescription = "describe";
        command.commandHelp = "help";
        command.pluginAPIHelp = "api";
        String markdown = ExecutionCommand.getExecutionCommandMarkdownString(command);
        Assert.assertTrue(markdown.contains("## Execution Command"));
        Assert.assertTrue(markdown.contains("Command Description"));
        Assert.assertTrue(markdown.contains("Plugin API Help"));
        Assert.assertEquals("null", ExecutionCommand.getExecutionCommandMarkdownString(null));
    }

    @Test
    public void getExecutionCommandMarkdownStringDefaultsCommandLabel() {
        ExecutionCommand command = new ExecutionCommand();
        String markdown = ExecutionCommand.getExecutionCommandMarkdownString(command);
        Assert.assertEquals("Execution Command", command.commandLabel);
        Assert.assertTrue(markdown.contains("## Execution Command"));
    }

    @Test
    public void individualLogStringGettersExposeValues() {
        ExecutionCommand command = new ExecutionCommand(11, "/bin/sh", new String[]{"a"}, "in",
            "/dir", Runner.APP_SHELL.getName(), true);
        command.mPid = 12;
        command.sessionAction = "0";
        command.shellName = "name";
        command.shellCreateMode = "always";
        command.commandDescription = "desc";
        command.commandHelp = "help";
        command.pluginAPIHelp = "apihelp";
        Assert.assertEquals("(11) ", command.getIdLogString());
        Assert.assertTrue(command.getPidLogString().contains("12"));
        Assert.assertTrue(command.getCurrentStateLogString().contains("Pre-Execution"));
        Assert.assertTrue(command.getPreviousStateLogString().contains("Pre-Execution"));
        ExecutionCommand labelled = new ExecutionCommand(11);
        labelled.commandLabel = "Demo";
        Assert.assertEquals("(11) Demo", labelled.getCommandIdAndLabelLogString());
        Assert.assertTrue(command.getExecutableLogString().contains("/bin/sh"));
        Assert.assertTrue(command.getArgumentsLogString().contains("Arg 1"));
        Assert.assertTrue(command.getWorkingDirectoryLogString().contains("/dir"));
        Assert.assertTrue(command.getRunnerLogString().contains("app-shell"));
        Assert.assertTrue(command.getIsFailsafeLogString().contains("true"));
        Assert.assertTrue(command.getStdinLogString().contains("Stdin"));
        Assert.assertTrue(command.getBackgroundCustomLogLevelLogString().contains("Background"));
        Assert.assertTrue(command.getSessionActionLogString().contains("Session Action"));
        Assert.assertTrue(command.getShellNameLogString().contains("name"));
        Assert.assertTrue(command.getShellCreateModeLogString().contains("always"));
        Assert.assertTrue(command.getSetRunnerShellEnvironmentLogString().contains("Environment"));
        Assert.assertTrue(command.getCommandDescriptionLogString().contains("desc"));
        Assert.assertTrue(command.getCommandHelpLogString().contains("help"));
        Assert.assertTrue(command.getPluginAPIHelpLogString().contains("apihelp"));
        Assert.assertEquals("Command Intent: -", command.getCommandIntentLogString());
    }

    @Test
    public void getIdLogStringIsEmptyWhenIdIsNull() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertEquals("", command.getIdLogString());
        Assert.assertEquals("Execution Command", command.getCommandLabelLogString());
    }

    @Test
    public void getStdinLogStringForEmptyStdin() {
        ExecutionCommand command = new ExecutionCommand();
        Assert.assertEquals("Stdin: -", command.getStdinLogString());
    }

    @Test
    public void argumentsLogStringHandlesPopulatedAndEmptyArrays() {
        String populated = ExecutionCommand.getArgumentsLogString("Arguments", new String[]{"one", "two"});
        Assert.assertTrue(populated.contains("Arg 1"));
        Assert.assertTrue(populated.contains("Arg 2"));
        Assert.assertEquals("Arguments: -", ExecutionCommand.getArgumentsLogString("Arguments", new String[]{}));
        Assert.assertEquals("Arguments: -", ExecutionCommand.getArgumentsLogString("Arguments", null));
    }

    @Test
    public void argumentsMarkdownStringHandlesPopulatedAndEmptyArrays() {
        String populated = ExecutionCommand.getArgumentsMarkdownString("Arguments", new String[]{"one"});
        Assert.assertTrue(populated.contains("Arg 1"));
        Assert.assertTrue(ExecutionCommand.getArgumentsMarkdownString("Arguments", null).contains("Arguments"));
    }
}
