package com.termux.shared.shell.command.result;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.termux.shared.data.DataUtils;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ResultSenderPendingIntentTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private PendingIntent newPendingIntent() {
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE
            : 0;
        return PendingIntent.getBroadcast(context(), 0, new Intent("com.termux.test.RESULT"), flags);
    }

    private ResultConfig pendingIntentConfig() {
        ResultConfig config = new ResultConfig();
        config.resultPendingIntent = newPendingIntent();
        config.resultBundleKey = "result";
        config.resultStdoutKey = "stdout";
        config.resultStdoutOriginalLengthKey = "stdout_original_length";
        config.resultStderrKey = "stderr";
        config.resultStderrOriginalLengthKey = "stderr_original_length";
        config.resultExitCodeKey = "exit_code";
        config.resultErrCodeKey = "err";
        config.resultErrmsgKey = "errmsg";
        return config;
    }

    @Test
    public void sendWithPendingIntentReturnsNullOnSuccessfulDelivery() {
        ResultData resultData = new ResultData();
        resultData.stdout.append("hello");
        resultData.exitCode = 0;
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            pendingIntentConfig(), resultData, false);
        Assert.assertNull(error);
    }

    @Test
    public void sendWithPendingIntentReturnsErrorWhenBundleKeyMissing() {
        ResultConfig config = pendingIntentConfig();
        config.resultBundleKey = null;
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendWithPendingIntentHandlesStdoutOnlyTruncationBranch() {
        ResultData resultData = new ResultData();
        StringBuilder largeStdout = new StringBuilder();
        for (int i = 0; i < DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES + 100; i++) {
            largeStdout.append('x');
        }
        resultData.stdout.append(largeStdout);
        resultData.exitCode = 0;
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), null, "label",
            pendingIntentConfig(), resultData, true);
        Assert.assertNull(error);
    }

    @Test
    public void sendWithPendingIntentHandlesStderrOnlyTruncationBranch() {
        ResultData resultData = new ResultData();
        StringBuilder largeStderr = new StringBuilder();
        for (int i = 0; i < DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES + 100; i++) {
            largeStderr.append('y');
        }
        resultData.stderr.append(largeStderr);
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            pendingIntentConfig(), resultData, false);
        Assert.assertNull(error);
    }

    @Test
    public void sendWithPendingIntentHandlesBothStreamsTruncationBranch() {
        ResultData resultData = new ResultData();
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES; i++) {
            large.append('z');
        }
        resultData.stdout.append(large);
        resultData.stderr.append(large);
        resultData.exitCode = 9;
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            pendingIntentConfig(), resultData, true);
        Assert.assertNull(error);
    }

    @Test
    public void sendWithPendingIntentIncludesErrmsgWhenStateFailed() {
        ResultData resultData = new ResultData();
        resultData.stdout.append("partial");
        resultData.setStateFailed(100, "failure detail");
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            pendingIntentConfig(), resultData, false);
        Assert.assertNull(error);
    }

    @Test
    public void sendCommandResultDataRoutesToPendingIntentWhenOnlyIntentSet() {
        ResultData resultData = new ResultData();
        resultData.stdout.append("routed");
        resultData.exitCode = 0;
        Error error = ResultSender.sendCommandResultData(context(), "tag", "label",
            pendingIntentConfig(), resultData, false);
        Assert.assertNull(error);
    }
}
