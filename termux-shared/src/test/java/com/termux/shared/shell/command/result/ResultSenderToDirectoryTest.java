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
public class ResultSenderToDirectoryTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "rsdir_" + System.nanoTime());
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
    public void singleFileSuccessReturnsErrorForInvalidCustomOutputFormat() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = true;
        config.resultFileBasename = "result.txt";
        config.resultFileOutputFormat = "%9$s";

        ResultData resultData = new ResultData();
        resultData.stdout.append("body");
        resultData.exitCode = 0;

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, resultData, false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_FORMAT_RESULT_OUTPUT_FAILED_WITH_EXCEPTION.getCode(),
            (int) error.getCode());
    }

    @Test
    public void singleFileFailureReturnsErrorForInvalidCustomErrorFormat() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = true;
        config.resultFileBasename = "result.txt";
        config.resultFileErrorFormat = "%9$s";

        ResultData resultData = new ResultData();
        resultData.setStateFailed(100, "boom");

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, resultData, false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_FORMAT_RESULT_ERROR_FAILED_WITH_EXCEPTION.getCode(),
            (int) error.getCode());
    }

    @Test
    public void singleFileModeDefaultsSuffixlessBasenameWhenBlank() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = true;
        config.resultFileBasename = "";

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_RESULT_FILE_BASENAME_NULL_OR_INVALID.getCode(),
            (int) error.getCode());
    }

    @Test
    public void multiFileModeDefaultsNullSuffixToEmptyBeforeValidation() {
        File dir = newTempDir();
        ResultConfig config = directoryConfig(dir);
        config.resultSingleFile = false;
        config.resultFilesSuffix = "still/invalid";

        Error error = ResultSender.sendCommandResultDataToDirectory(context(), "tag", "label",
            config, new ResultData(), false);
        Assert.assertNotNull(error);
        Assert.assertEquals(ResultSenderErrno.ERROR_RESULT_FILES_SUFFIX_INVALID.getCode(),
            (int) error.getCode());
    }
}
