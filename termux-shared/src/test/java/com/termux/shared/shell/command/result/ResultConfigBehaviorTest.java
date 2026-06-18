package com.termux.shared.shell.command.result;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ResultConfigBehaviorTest {

    private static PendingIntent newPendingIntent() {
        Context context = RuntimeEnvironment.getApplication();
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE
            : 0;
        return PendingIntent.getBroadcast(context, 0,
            new Intent("com.termux.test.RESULT"), flags);
    }

    @Test
    public void isCommandWithPendingResultTrueWhenPendingIntentSet() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();
        Assert.assertTrue(config.isCommandWithPendingResult());
    }

    @Test
    public void getResultPendingIntentVariablesLogStringUsesDashWhenIntentNull() {
        Assert.assertEquals("Result PendingIntent Creator: -",
            new ResultConfig().getResultPendingIntentVariablesLogString(true));
    }

    @Test
    public void getResultPendingIntentVariablesLogStringOmitsNullKeysWhenIgnoreNull() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();

        String logString = config.getResultPendingIntentVariablesLogString(true);
        Assert.assertTrue(logString.contains("Result PendingIntent Creator"));
        Assert.assertFalse(logString.contains("Result Bundle Key"));
    }

    @Test
    public void getResultPendingIntentVariablesLogStringIncludesAllKeysWhenSet() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();
        config.resultBundleKey = "bundle";
        config.resultStdoutKey = "stdout";
        config.resultStderrKey = "stderr";
        config.resultExitCodeKey = "exit";
        config.resultErrCodeKey = "errcode";
        config.resultErrmsgKey = "errmsg";
        config.resultStdoutOriginalLengthKey = "stdoutlen";
        config.resultStderrOriginalLengthKey = "stderrlen";

        String logString = config.getResultPendingIntentVariablesLogString(true);
        Assert.assertTrue(logString.contains("Result Bundle Key"));
        Assert.assertTrue(logString.contains("Result Stdout Key"));
        Assert.assertTrue(logString.contains("Result Stderr Key"));
        Assert.assertTrue(logString.contains("Result Exit Code Key"));
        Assert.assertTrue(logString.contains("Result Err Code Key"));
        Assert.assertTrue(logString.contains("Result Error Key"));
        Assert.assertTrue(logString.contains("Result Stdout Original Length Key"));
        Assert.assertTrue(logString.contains("Result Stderr Original Length Key"));
    }

    @Test
    public void getResultPendingIntentVariablesLogStringIncludesNullKeysWhenNotIgnoreNull() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();

        String logString = config.getResultPendingIntentVariablesLogString(false);
        Assert.assertTrue(logString.contains("Result Bundle Key"));
        Assert.assertTrue(logString.contains("Result Stderr Original Length Key"));
    }

    @Test
    public void getResultConfigLogStringCombinesPendingIntentAndDirectorySections() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();
        config.resultDirectoryPath = "/tmp/result";

        String logString = ResultConfig.getResultConfigLogString(config, true);
        Assert.assertTrue(logString.contains("Result Pending: `true`"));
        Assert.assertTrue(logString.contains("Result PendingIntent Creator"));
        Assert.assertTrue(logString.contains("Result Directory Path"));
    }

    @Test
    public void getResultDirectoryVariablesLogStringIncludesOutputErrorFormatAndSuffixWhenSet() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        config.resultFileOutputFormat = "out-format";
        config.resultFileErrorFormat = "err-format";
        config.resultFilesSuffix = "suffix";

        String logString = config.getResultDirectoryVariablesLogString(true);
        Assert.assertTrue(logString.contains("Result File Output Format"));
        Assert.assertTrue(logString.contains("Result File Error Format"));
        Assert.assertTrue(logString.contains("Result Files Suffix"));
    }

    @Test
    public void getResultConfigMarkdownStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultConfig.getResultConfigMarkdownString(null));
    }

    @Test
    public void getResultConfigMarkdownStringUsesDashCreatorWhenNoPendingIntent() {
        String markdownString = ResultConfig.getResultConfigMarkdownString(new ResultConfig());
        Assert.assertTrue(markdownString.contains("Result PendingIntent Creator"));
    }

    @Test
    public void getResultConfigMarkdownStringIncludesDirectoryFieldsWhenDirectorySet() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        config.resultSingleFile = true;
        config.resultFileBasename = "out.txt";

        String markdownString = ResultConfig.getResultConfigMarkdownString(config);
        Assert.assertTrue(markdownString.contains("Result Directory Path"));
        Assert.assertTrue(markdownString.contains("Result Single File"));
        Assert.assertTrue(markdownString.contains("Result File Basename"));
    }

    @Test
    public void getResultConfigMarkdownStringIncludesCreatorEntryWhenPendingIntentSet() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();

        String markdownString = ResultConfig.getResultConfigMarkdownString(config);
        Assert.assertTrue(markdownString.contains("Result PendingIntent Creator"));
    }
}
