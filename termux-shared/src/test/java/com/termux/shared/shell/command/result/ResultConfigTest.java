package com.termux.shared.shell.command.result;

import org.junit.Assert;
import org.junit.Test;

public class ResultConfigTest {

    @Test
    public void newResultConfigHasNoPendingResult() {
        Assert.assertFalse(new ResultConfig().isCommandWithPendingResult());
    }

    @Test
    public void isCommandWithPendingResultTrueWhenDirectoryPathSet() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        Assert.assertTrue(config.isCommandWithPendingResult());
    }

    @Test
    public void getResultConfigLogStringStaticReturnsNullLiteralForNull() {
        Assert.assertEquals("null", ResultConfig.getResultConfigLogString(null, true));
    }

    @Test
    public void getResultConfigLogStringReportsPendingFalseWhenNothingSet() {
        String logString = ResultConfig.getResultConfigLogString(new ResultConfig(), true);
        Assert.assertTrue(logString.contains("Result Pending: `false`"));
    }

    @Test
    public void getResultConfigLogStringReportsPendingTrueWhenDirectorySet() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        String logString = ResultConfig.getResultConfigLogString(config, true);
        Assert.assertTrue(logString.contains("Result Pending: `true`"));
        Assert.assertTrue(logString.contains("Result Directory Path"));
    }

    @Test
    public void getResultDirectoryVariablesLogStringUsesDashWhenPathNull() {
        Assert.assertEquals("Result Directory Path: -",
            new ResultConfig().getResultDirectoryVariablesLogString(true));
    }

    @Test
    public void getResultDirectoryVariablesLogStringIncludesSetBasenameWhenPresent() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        config.resultFileBasename = "out.txt";
        String logString = config.getResultDirectoryVariablesLogString(true);
        Assert.assertTrue(logString.contains("Result File Basename"));
        Assert.assertTrue(logString.contains("out.txt"));
    }

    @Test
    public void getResultDirectoryVariablesLogStringOmitsNullBasenameWhenIgnoreNull() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        String logString = config.getResultDirectoryVariablesLogString(true);
        Assert.assertFalse(logString.contains("Result File Basename"));
    }

    @Test
    public void getResultDirectoryVariablesLogStringIncludesNullBasenameWhenNotIgnoreNull() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/tmp/result";
        String logString = config.getResultDirectoryVariablesLogString(false);
        Assert.assertTrue(logString.contains("Result File Basename"));
    }

    @Test
    public void toStringContainsResultPendingMarker() {
        Assert.assertTrue(new ResultConfig().toString().contains("Result Pending"));
    }
}
