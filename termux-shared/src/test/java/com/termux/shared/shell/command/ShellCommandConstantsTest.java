package com.termux.shared.shell.command;

import com.termux.shared.shell.command.ShellCommandConstants.RESULT_SENDER;

import org.junit.Assert;
import org.junit.Test;

public class ShellCommandConstantsTest {

    @Test
    public void successStdoutFormatEmitsStdoutFollowedByNewline() {
        String formatted = String.format(RESULT_SENDER.FORMAT_SUCCESS_STDOUT, "payload");
        Assert.assertEquals("payload" + System.lineSeparator(), formatted);
    }

    @Test
    public void successStdoutAndExitCodeFormatEmitsBothFields() {
        String formatted = String.format(RESULT_SENDER.FORMAT_SUCCESS_STDOUT__EXIT_CODE, "out", "5");
        Assert.assertTrue(formatted.contains("out"));
        Assert.assertTrue(formatted.contains("exit_code=5"));
    }

    @Test
    public void successStdoutStderrExitCodeFormatEmitsAllSections() {
        String formatted = String.format(RESULT_SENDER.FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE,
            "outval", "errval", "0");
        Assert.assertTrue(formatted.contains("stdout="));
        Assert.assertTrue(formatted.contains("outval"));
        Assert.assertTrue(formatted.contains("stderr="));
        Assert.assertTrue(formatted.contains("errval"));
        Assert.assertTrue(formatted.contains("exit_code=0"));
    }

    @Test
    public void failedFormatEmitsAllFiveFields() {
        String formatted = String.format(RESULT_SENDER.FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE,
            "100", "message", "outval", "errval", "1");
        Assert.assertTrue(formatted.contains("err=100"));
        Assert.assertTrue(formatted.contains("errmsg="));
        Assert.assertTrue(formatted.contains("message"));
        Assert.assertTrue(formatted.contains("stdout="));
        Assert.assertTrue(formatted.contains("stderr="));
        Assert.assertTrue(formatted.contains("exit_code=1"));
    }

    @Test
    public void resultFilePrefixesAreDistinctAcrossAllStreams() {
        String[] prefixes = {
            RESULT_SENDER.RESULT_FILE_ERR_PREFIX,
            RESULT_SENDER.RESULT_FILE_ERRMSG_PREFIX,
            RESULT_SENDER.RESULT_FILE_STDOUT_PREFIX,
            RESULT_SENDER.RESULT_FILE_STDERR_PREFIX,
            RESULT_SENDER.RESULT_FILE_EXIT_CODE_PREFIX,
        };
        for (int i = 0; i < prefixes.length; i++) {
            for (int j = i + 1; j < prefixes.length; j++) {
                Assert.assertNotEquals(prefixes[i], prefixes[j]);
            }
        }
    }
}
