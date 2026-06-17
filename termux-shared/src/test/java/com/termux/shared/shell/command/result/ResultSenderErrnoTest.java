package com.termux.shared.shell.command.result;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class ResultSenderErrnoTest {

    @Test
    public void typeConstantIsResultSenderError() {
        Assert.assertEquals("ResultSender Error", ResultSenderErrno.TYPE);
        Assert.assertEquals(ResultSenderErrno.TYPE, ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getType());
    }

    @Test
    public void resultFileBasenameErrnoExposesCodeAndMessage() {
        Assert.assertEquals(100, ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getCode());
        Assert.assertTrue(ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getMessage().contains("forward slashes"));
    }

    @Test
    public void formatResultErrnosHaveDistinctCodes() {
        Assert.assertEquals(101, ResultSenderErrno.ERROR_RESULT_FILES_SUFFIX_INVALID.getCode());
        Assert.assertEquals(102, ResultSenderErrno.ERROR_FORMAT_RESULT_ERROR_FAILED_WITH_EXCEPTION.getCode());
        Assert.assertEquals(103, ResultSenderErrno.ERROR_FORMAT_RESULT_OUTPUT_FAILED_WITH_EXCEPTION.getCode());
    }

    @Test
    public void getErrorFormatsBasenameArgument() {
        Error error = ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getError("a/b");
        Assert.assertEquals(ResultSenderErrno.TYPE, error.getType());
        Assert.assertTrue(error.getMessage().contains("\"a/b\""));
    }

    @Test
    public void valueOfResolvesRegisteredResultSenderErrno() {
        Errno expected = ResultSenderErrno.ERROR_FORMAT_RESULT_OUTPUT_FAILED_WITH_EXCEPTION;
        Errno resolved = Errno.valueOf(ResultSenderErrno.TYPE, 103);
        Assert.assertSame(expected, resolved);
    }
}
