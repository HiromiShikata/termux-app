package com.termux.shared.shell.command.result;

import android.content.Context;
import android.os.Build;

import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ResultSenderTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
            "rstest_" + System.nanoTime());
        Assert.assertTrue(dir.mkdirs());
        dir.deleteOnExit();
        return dir;
    }

    private ResultConfig directoryConfig(File dir) {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = dir.getAbsolutePath();
        config.resultDirectoryAllowedParentPath = dir.getParentFile().getAbsolutePath();
        return config;
    }

    @Test
    public void sendCommandResultDataReturnsErrorWhenContextNull() {
        Error error = ResultSender.sendCommandResultData(null, "tag", "label",
            new ResultConfig(), new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendCommandResultDataReturnsErrorWhenResultConfigNull() {
        Error error = ResultSender.sendCommandResultData(context(), "tag", "label",
            null, new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendCommandResultDataReturnsErrorWhenNoPendingIntentOrDirectory() {
        Error error = ResultSender.sendCommandResultData(context(), "tag", "label",
            new ResultConfig(), new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendCommandResultDataWithPendingIntentReturnsErrorWhenBundleKeyMissing() {
        ResultConfig config = new ResultConfig();
        Error error = ResultSender.sendCommandResultDataWithPendingIntent(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendCommandResultDataToDirectoryReturnsErrorWhenContextNull() {
        File dir = newTempDir();
        Error error = ResultSender.sendCommandResultDataToDirectory(null, "tag", "label",
            directoryConfig(dir), new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void sendCommandResultDataToDirectoryReturnsErrorWhenDirectoryPathEmpty() {
        ResultConfig config = new ResultConfig();
        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
    }

    @Test
    public void multiFileModeRejectsSuffixWithSlash() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = false;
        config.resultFilesSuffix = "bad/suffix";

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_RESULT_FILES_SUFFIX_INVALID.getCode(), (int) error.getCode());
    }

    @Test
    public void singleFileModeRejectsBasenameWithSlash() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = true;
        config.resultFileBasename = "nested/result.txt";

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getCode(), (int) error.getCode());
    }

    @Test
    public void singleFileModeRejectsNullBasename() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = true;
        config.resultFileBasename = null;

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getCode(), (int) error.getCode());
    }

    @Test
    public void sendCommandResultDataToDirectoryReturnsErrorForUnwritableDirectory() {
        ResultConfig config = new ResultConfig();
        config.resultDirectoryPath = "/proc/nonexistent-result-dir";
        config.resultDirectoryAllowedParentPath = "/proc";
        config.resultSingleFile = false;

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
    }
}
