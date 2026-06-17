package com.termux.shared.termux.plugins;

import android.content.Context;
import android.os.Build;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.result.ResultConfig;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TermuxPluginUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void setPluginResultPendingIntentVariablesPopulatesBundleKeys() {
        ExecutionCommand command = new ExecutionCommand(1);
        TermuxPluginUtils.setPluginResultPendingIntentVariables(command);

        ResultConfig config = command.resultConfig;
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE, config.resultBundleKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT, config.resultStdoutKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH, config.resultStdoutOriginalLengthKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR, config.resultStderrKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH, config.resultStderrOriginalLengthKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, config.resultExitCodeKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR, config.resultErrCodeKey);
        Assert.assertEquals(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG, config.resultErrmsgKey);
    }

    @Test
    public void setPluginResultDirectoryVariablesCanonicalizesPathAndDerivesBasename() {
        ExecutionCommand command = new ExecutionCommand(2);
        command.executable = "/path/to/myscript.sh";
        command.resultConfig.resultDirectoryPath = "/data/data/com.termux/files/home/output";
        command.resultConfig.resultSingleFile = true;

        TermuxPluginUtils.setPluginResultDirectoryVariables(command);

        Assert.assertNotNull(command.resultConfig.resultDirectoryPath);
        Assert.assertNotNull(command.resultConfig.resultFileBasename);
        Assert.assertTrue(command.resultConfig.resultFileBasename.startsWith("myscript"));
        Assert.assertTrue(command.resultConfig.resultFileBasename.endsWith(".log"));
    }

    @Test
    public void setPluginResultDirectoryVariablesKeepsExistingBasename() {
        ExecutionCommand command = new ExecutionCommand(3);
        command.executable = "/path/to/myscript.sh";
        command.resultConfig.resultDirectoryPath = "/data/data/com.termux/files/home/output";
        command.resultConfig.resultSingleFile = true;
        command.resultConfig.resultFileBasename = "preset.txt";

        TermuxPluginUtils.setPluginResultDirectoryVariables(command);

        Assert.assertEquals("preset.txt", command.resultConfig.resultFileBasename);
    }

    @Test
    public void checkIfAllowExternalAppsPolicyIsViolatedReturnsErrorWhenPropertiesUnset() {
        String error = TermuxPluginUtils.checkIfAllowExternalAppsPolicyIsViolated(context(), "RunCommand");
        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("RunCommand"));
    }

    @Test
    public void processPluginExecutionCommandResultWithNullCommandIsNoOp() {
        TermuxPluginUtils.processPluginExecutionCommandResult(context(), "tag", null);
    }

    @Test
    public void processPluginExecutionCommandResultIgnoresUnexecutedCommand() {
        ExecutionCommand command = new ExecutionCommand(4);
        TermuxPluginUtils.processPluginExecutionCommandResult(context(), "tag", command);
        Assert.assertFalse(command.hasExecuted());
    }

    @Test
    public void processPluginExecutionCommandErrorWithNullContextIsNoOp() {
        ExecutionCommand command = new ExecutionCommand(5);
        TermuxPluginUtils.processPluginExecutionCommandError(null, "tag", command, false);
    }

    @Test
    public void processPluginExecutionCommandErrorIgnoresCommandNotInFailedState() {
        ExecutionCommand command = new ExecutionCommand(6);
        TermuxPluginUtils.processPluginExecutionCommandError(context(), "tag", command, false);
        Assert.assertFalse(command.isStateFailed());
    }

    @Test
    public void sendPluginCommandErrorNotificationWithNullContextIsNoOp() {
        TermuxPluginUtils.sendPluginCommandErrorNotification(null, "tag", "title", "message", new RuntimeException("x"));
    }

    @Test
    public void setupPluginCommandErrorsNotificationChannelDoesNotThrow() {
        TermuxPluginUtils.setupPluginCommandErrorsNotificationChannel(context());
    }
}
