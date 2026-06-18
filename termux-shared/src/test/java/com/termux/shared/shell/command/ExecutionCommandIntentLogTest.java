package com.termux.shared.shell.command;

import android.content.Intent;
import android.os.Build;

import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand.ExecutionState;
import com.termux.shared.shell.command.ExecutionCommand.Runner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ExecutionCommandIntentLogTest {

    @Test
    public void commandIntentLogStringRendersIntentDetailsWhenSet() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
        ExecutionCommand command = new ExecutionCommand(1);
        command.commandIntent = new Intent("com.termux.action.RUN");
        command.commandIntent.putExtra("argument", "demo");
        String logString = command.getCommandIntentLogString();
        Assert.assertTrue(logString.contains("Command Intent"));
        Assert.assertTrue(logString.contains("argument: `demo`"));
    }

    @Test
    public void inputLogStringRendersPreviousStateAfterAdvancing() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
        ExecutionCommand command = new ExecutionCommand(2, "/bin/sh", null, null, "/work",
            Runner.TERMINAL_SESSION.getName(), false);
        command.setState(ExecutionState.EXECUTING);
        command.setState(ExecutionState.EXECUTED);
        String logString = ExecutionCommand.getExecutionInputLogString(command, true, true);
        Assert.assertTrue(logString.contains("Previous State: `Executing`"));
        Assert.assertTrue(logString.contains("Current State: `Executed`"));
    }

    @Test
    public void markdownStringRendersCommandIntentBackedExecution() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
        ExecutionCommand command = new ExecutionCommand(3, "/bin/sh", new String[]{"-c", "id"},
            "stdin-text", "/work", Runner.APP_SHELL.getName(), false);
        command.isPluginExecutionCommand = true;
        command.commandIntent = new Intent("com.termux.action.RUN");
        String markdown = ExecutionCommand.getExecutionCommandMarkdownString(command);
        Assert.assertTrue(markdown.contains("Stdin"));
        Assert.assertTrue(markdown.contains("isPluginExecutionCommand"));
    }
}
