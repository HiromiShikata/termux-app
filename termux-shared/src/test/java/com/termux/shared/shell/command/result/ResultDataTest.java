package com.termux.shared.shell.command.result;

import com.termux.shared.errors.Errno;

import org.junit.Assert;
import org.junit.Test;

public class ResultDataTest {

    @Test
    public void newResultDataHasEmptyStreamsAndSuccessErrCode() {
        ResultData resultData = new ResultData();
        Assert.assertEquals(0, resultData.stdout.length());
        Assert.assertEquals(0, resultData.stderr.length());
        Assert.assertFalse(resultData.isStateFailed());
        Assert.assertEquals(Errno.ERRNO_SUCCESS.getCode(), resultData.getErrCode());
    }

    @Test
    public void appendStdoutAddsToStdoutBuffer() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("hello");
        Assert.assertEquals("hello", resultData.stdout.toString());
    }

    @Test
    public void appendStdoutLnAddsTrailingNewline() {
        ResultData resultData = new ResultData();
        resultData.appendStdoutLn("line");
        Assert.assertEquals("line\n", resultData.stdout.toString());
    }

    @Test
    public void prependStdoutInsertsAtBeginning() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("world");
        resultData.prependStdout("hello ");
        Assert.assertEquals("hello world", resultData.stdout.toString());
    }

    @Test
    public void prependStdoutLnInsertsLineAtBeginning() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("body");
        resultData.prependStdoutLn("header");
        Assert.assertEquals("header\nbody", resultData.stdout.toString());
    }

    @Test
    public void clearStdoutEmptiesBuffer() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("data");
        resultData.clearStdout();
        Assert.assertEquals(0, resultData.stdout.length());
    }

    @Test
    public void appendStderrAndClearStderrWork() {
        ResultData resultData = new ResultData();
        resultData.appendStderrLn("err");
        Assert.assertEquals("err\n", resultData.stderr.toString());
        resultData.clearStderr();
        Assert.assertEquals(0, resultData.stderr.length());
    }

    @Test
    public void prependStderrInsertsAtBeginning() {
        ResultData resultData = new ResultData();
        resultData.appendStderr("tail");
        resultData.prependStderr("head ");
        Assert.assertEquals("head tail", resultData.stderr.toString());
    }

    @Test
    public void prependStderrLnInsertsLineAtBeginning() {
        ResultData resultData = new ResultData();
        resultData.appendStderr("body");
        resultData.prependStderrLn("header");
        Assert.assertEquals("header\nbody", resultData.stderr.toString());
    }

    @Test
    public void setStateFailedWithValidCodeMarksFailedAndRecordsErrCode() {
        ResultData resultData = new ResultData();
        boolean result = resultData.setStateFailed(Errno.ERRNO_FAILED.getCode(), "boom");
        Assert.assertTrue(result);
        Assert.assertTrue(resultData.isStateFailed());
        Assert.assertEquals(Errno.ERRNO_FAILED.getCode(), resultData.getErrCode());
    }

    @Test
    public void setStateFailedFromErrnoErrorPropagatesTypeCodeMessage() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(Errno.ERRNO_FAILED.getError());
        Assert.assertTrue(resultData.isStateFailed());
        Assert.assertEquals(Errno.ERRNO_FAILED.getCode(), resultData.getErrCode());
    }

    @Test
    public void getErrCodeReturnsLastRecordedErrorCode() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(Errno.ERRNO_FAILED.getCode(), "first");
        resultData.setStateFailed(Errno.ERRNO_MINOR_FAILURES.getCode(), "second");
        Assert.assertEquals(Errno.ERRNO_MINOR_FAILURES.getCode(), resultData.getErrCode());
    }

    @Test
    public void getResultDataLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultData.getResultDataLogString(null, true));
    }

    @Test
    public void getResultDataLogStringContainsExitCodeEntry() {
        ResultData resultData = new ResultData();
        resultData.exitCode = 0;
        String logString = ResultData.getResultDataLogString(resultData, true);
        Assert.assertTrue(logString.contains("Exit Code"));
        Assert.assertTrue(logString.contains("Stdout"));
        Assert.assertTrue(logString.contains("Stderr"));
    }

    @Test
    public void getExitCodeLogStringRendersExitCodeValue() {
        ResultData resultData = new ResultData();
        resultData.exitCode = 7;
        Assert.assertEquals("Exit Code: `7`", resultData.getExitCodeLogString());
    }

    @Test
    public void getStdoutLogStringUsesDashWhenEmpty() {
        ResultData resultData = new ResultData();
        Assert.assertEquals("Stdout: -", resultData.getStdoutLogString());
    }

    @Test
    public void getErrorsListLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultData.getErrorsListLogString(null));
    }

    @Test
    public void getErrorsListLogStringContainsRecordedError() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(Errno.ERRNO_FAILED.getCode(), "boom");
        Assert.assertTrue(ResultData.getErrorsListLogString(resultData).contains("boom"));
    }

    @Test
    public void getResultDataMarkdownStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultData.getResultDataMarkdownString(null));
    }

    @Test
    public void getResultDataMarkdownStringContainsExitCodeEntry() {
        ResultData resultData = new ResultData();
        resultData.exitCode = 0;
        Assert.assertTrue(ResultData.getResultDataMarkdownString(resultData).contains("Exit Code"));
    }

    @Test
    public void getErrorsListMinimalStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultData.getErrorsListMinimalString(null));
    }

    @Test
    public void getErrorsListMinimalStringContainsCodeAndMessage() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(Errno.ERRNO_FAILED.getCode(), "boom");
        String minimal = ResultData.getErrorsListMinimalString(resultData);
        Assert.assertTrue(minimal.contains("boom"));
        Assert.assertTrue(minimal.contains("(" + Errno.ERRNO_FAILED.getCode() + ")"));
    }
}
