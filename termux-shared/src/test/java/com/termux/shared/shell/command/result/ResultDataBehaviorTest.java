package com.termux.shared.shell.command.result;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ResultDataBehaviorTest {

    private static final int FAILED = Errno.ERRNO_FAILED.getCode();
    private static final int SUCCESS = Errno.ERRNO_SUCCESS.getCode();

    @Test
    public void appendStderrLnAddsTrailingNewline() {
        ResultData resultData = new ResultData();
        resultData.appendStderrLn("warning");
        Assert.assertEquals("warning\n", resultData.stderr.toString());
    }

    @Test
    public void prependStderrLnInsertsLineAtBeginning() {
        ResultData resultData = new ResultData();
        resultData.appendStderr("tail");
        resultData.prependStderrLn("head");
        Assert.assertEquals("head\ntail", resultData.stderr.toString());
    }

    @Test
    public void clearStdoutAfterAppendEmptiesBuffer() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("data");
        resultData.clearStdout();
        Assert.assertEquals(0, resultData.stdout.length());
    }

    @Test
    public void setStateFailedFromErrorWithThrowableRecordsErrorAndThrowable() {
        ResultData resultData = new ResultData();
        Error source = new Error("SrcType", FAILED, "source message");

        Assert.assertTrue(resultData.setStateFailed(source, new RuntimeException("boom")));
        Assert.assertTrue(resultData.isStateFailed());
        Assert.assertEquals(1, resultData.errorsList.size());
        Assert.assertEquals(1, resultData.errorsList.get(0).getThrowablesList().size());
    }

    @Test
    public void setStateFailedFromErrorWithThrowablesListRecordsThrowables() {
        ResultData resultData = new ResultData();
        Error source = new Error("SrcType", FAILED, "source message");
        List<Throwable> throwables = Arrays.asList(new RuntimeException("a"), new RuntimeException("b"));

        Assert.assertTrue(resultData.setStateFailed(source, throwables));
        Assert.assertEquals(2, resultData.errorsList.get(0).getThrowablesList().size());
    }

    @Test
    public void setStateFailedWithCodeMessageThrowableRecordsThrowable() {
        ResultData resultData = new ResultData();

        Assert.assertTrue(resultData.setStateFailed(FAILED, "msg", new RuntimeException("boom")));
        Assert.assertEquals(1, resultData.errorsList.get(0).getThrowablesList().size());
    }

    @Test
    public void setStateFailedWithCodeMessageThrowablesListRecordsThrowables() {
        ResultData resultData = new ResultData();

        Assert.assertTrue(resultData.setStateFailed(FAILED, "msg",
            Collections.singletonList(new RuntimeException("boom"))));
        Assert.assertEquals(1, resultData.errorsList.get(0).getThrowablesList().size());
    }

    @Test
    public void multipleErrorsAreAllRecordedAndStateIsFailed() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(FAILED, "first");
        resultData.setStateFailed(FAILED + 1, "second");

        Assert.assertEquals(2, resultData.errorsList.size());
        Assert.assertTrue(resultData.isStateFailed());
        Assert.assertEquals(FAILED + 1, resultData.getErrCode());
    }

    @Test
    public void isStateFailedRemainsFalseForFreshResultData() {
        ResultData resultData = new ResultData();
        Assert.assertFalse(resultData.isStateFailed());
        Assert.assertEquals(SUCCESS, resultData.getErrCode());
    }

    @Test
    public void getStdoutLogStringIncludesContentWhenNotEmpty() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("stdout-marker");
        String logString = resultData.getStdoutLogString();
        Assert.assertTrue(logString.contains("Stdout"));
        Assert.assertTrue(logString.contains("stdout-marker"));
    }

    @Test
    public void getStderrLogStringIncludesContentWhenNotEmpty() {
        ResultData resultData = new ResultData();
        resultData.appendStderr("stderr-marker");
        String logString = resultData.getStderrLogString();
        Assert.assertTrue(logString.contains("Stderr"));
        Assert.assertTrue(logString.contains("stderr-marker"));
    }

    @Test
    public void getStderrLogStringUsesDashWhenEmpty() {
        ResultData resultData = new ResultData();
        Assert.assertTrue(resultData.getStderrLogString().contains("Stderr"));
    }

    @Test
    public void toStringDelegatesToResultDataLogString() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("content");
        Assert.assertEquals(ResultData.getResultDataLogString(resultData, true), resultData.toString());
    }

    @Test
    public void getResultDataLogStringWithoutStreamsOmitsStdoutSection() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("hidden-marker");
        String logString = ResultData.getResultDataLogString(resultData, false);
        Assert.assertFalse(logString.contains("hidden-marker"));
        Assert.assertTrue(logString.contains("Exit Code"));
    }

    @Test
    public void getResultDataMarkdownStringContainsPopulatedStreamsAndError() {
        ResultData resultData = new ResultData();
        resultData.appendStdout("md-stdout");
        resultData.appendStderr("md-stderr");
        resultData.exitCode = 1;
        resultData.setStateFailed(FAILED, "md-error");

        String markdownString = ResultData.getResultDataMarkdownString(resultData);
        Assert.assertTrue(markdownString.contains("md-stdout"));
        Assert.assertTrue(markdownString.contains("md-stderr"));
        Assert.assertTrue(markdownString.contains("md-error"));
    }

    @Test
    public void getErrorsListMarkdownStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultData.getErrorsListMarkdownString(null));
    }

    @Test
    public void getErrorsListMarkdownStringContainsRecordedErrors() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(FAILED, "first-error");
        resultData.setStateFailed(FAILED, "second-error");

        String markdownString = ResultData.getErrorsListMarkdownString(resultData);
        Assert.assertTrue(markdownString.contains("first-error"));
        Assert.assertTrue(markdownString.contains("second-error"));
    }

    @Test
    public void getErrorsListLogStringContainsMultipleErrors() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(FAILED, "log-error-one");
        resultData.setStateFailed(FAILED, "log-error-two");

        String logString = ResultData.getErrorsListLogString(resultData);
        Assert.assertTrue(logString.contains("log-error-one"));
        Assert.assertTrue(logString.contains("log-error-two"));
    }

    @Test
    public void getErrorsListMinimalStringContainsMultipleErrors() {
        ResultData resultData = new ResultData();
        resultData.setStateFailed(FAILED, "minimal-one");
        resultData.setStateFailed(FAILED, "minimal-two");

        String minimalString = ResultData.getErrorsListMinimalString(resultData);
        Assert.assertTrue(minimalString.contains("minimal-one"));
        Assert.assertTrue(minimalString.contains("minimal-two"));
    }
}
