package com.termux.shared.termux.file;

import android.os.Build;
import android.os.Environment;

import com.termux.shared.errors.Error;
import com.termux.shared.termux.TermuxConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TermuxFileUtilsTest {

    @Test
    public void getExpandedTermuxPathReturnsNullForNull() {
        Assert.assertNull(TermuxFileUtils.getExpandedTermuxPath(null));
    }

    @Test
    public void getExpandedTermuxPathReturnsEmptyForEmpty() {
        Assert.assertEquals("", TermuxFileUtils.getExpandedTermuxPath(""));
    }

    @Test
    public void getExpandedTermuxPathExpandsBarePrefix() {
        Assert.assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH,
            TermuxFileUtils.getExpandedTermuxPath("$PREFIX"));
    }

    @Test
    public void getExpandedTermuxPathExpandsPrefixWithSubPath() {
        Assert.assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash",
            TermuxFileUtils.getExpandedTermuxPath("$PREFIX/bin/bash"));
    }

    @Test
    public void getExpandedTermuxPathExpandsHomeWithSubPath() {
        Assert.assertEquals(TermuxConstants.TERMUX_HOME_DIR_PATH + "/file.txt",
            TermuxFileUtils.getExpandedTermuxPath("~/file.txt"));
    }

    @Test
    public void getExpandedTermuxPathLeavesUnrelatedPathUnchanged() {
        Assert.assertEquals("/usr/local/bin", TermuxFileUtils.getExpandedTermuxPath("/usr/local/bin"));
    }

    @Test
    public void getExpandedTermuxPathsExpandsEachEntry() {
        List<String> result = TermuxFileUtils.getExpandedTermuxPaths(Arrays.asList("$PREFIX/bin", "~/notes"));
        Assert.assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin", result.get(0));
        Assert.assertEquals(TermuxConstants.TERMUX_HOME_DIR_PATH + "/notes", result.get(1));
    }

    @Test
    public void getExpandedTermuxPathsReturnsNullForNull() {
        Assert.assertNull(TermuxFileUtils.getExpandedTermuxPaths(null));
    }

    @Test
    public void getUnExpandedTermuxPathReturnsNullForNull() {
        Assert.assertNull(TermuxFileUtils.getUnExpandedTermuxPath(null));
    }

    @Test
    public void getUnExpandedTermuxPathReplacesPrefixPathWithVariable() {
        Assert.assertEquals("$PREFIX/bin/bash",
            TermuxFileUtils.getUnExpandedTermuxPath(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash"));
    }

    @Test
    public void getUnExpandedTermuxPathReplacesHomePathWithTilde() {
        Assert.assertEquals("~/file.txt",
            TermuxFileUtils.getUnExpandedTermuxPath(TermuxConstants.TERMUX_HOME_DIR_PATH + "/file.txt"));
    }

    @Test
    public void getUnExpandedTermuxPathsUnExpandsEachEntry() {
        List<String> result = TermuxFileUtils.getUnExpandedTermuxPaths(
            Arrays.asList(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin", TermuxConstants.TERMUX_HOME_DIR_PATH + "/x"));
        Assert.assertEquals("$PREFIX/bin", result.get(0));
        Assert.assertEquals("~/x", result.get(1));
    }

    @Test
    public void getUnExpandedTermuxPathsReturnsNullForNull() {
        Assert.assertNull(TermuxFileUtils.getUnExpandedTermuxPaths(null));
    }

    @Test
    public void expandThenUnExpandRoundTrips() {
        String original = "$PREFIX/lib/libc.so";
        String expanded = TermuxFileUtils.getExpandedTermuxPath(original);
        Assert.assertEquals(original, TermuxFileUtils.getUnExpandedTermuxPath(expanded));
    }

    @Test
    public void getCanonicalPathExpandsAndCanonicalizesPrefix() {
        Assert.assertEquals(TermuxConstants.TERMUX_PREFIX_DIR_PATH,
            TermuxFileUtils.getCanonicalPath("$PREFIX", null, true));
    }

    @Test
    public void getCanonicalPathReturnsAbsolutePathForNullInput() {
        String result = TermuxFileUtils.getCanonicalPath(null, null, false);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.startsWith("/"));
    }

    @Test
    public void getCanonicalPathWithoutExpandTreatsVariableLiterally() {
        String result = TermuxFileUtils.getCanonicalPath("$PREFIX/bin", null, false);
        Assert.assertTrue(result.startsWith("/"));
        Assert.assertTrue(result.endsWith("/$PREFIX/bin"));
    }

    @Test
    public void getMatchedAllowedParentReturnsFilesDirForNull() {
        Assert.assertEquals(TermuxConstants.TERMUX_FILES_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath(null));
    }

    @Test
    public void getMatchedAllowedParentReturnsFilesDirForEmpty() {
        Assert.assertEquals(TermuxConstants.TERMUX_FILES_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath(""));
    }

    @Test
    public void getMatchedAllowedParentReturnsStorageHomeForStoragePath() {
        Assert.assertEquals(TermuxConstants.TERMUX_STORAGE_HOME_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath(
                TermuxConstants.TERMUX_STORAGE_HOME_DIR_PATH + "/downloads"));
    }

    @Test
    public void getMatchedAllowedParentReturnsExternalStorageForExternalStoragePath() {
        String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        Assert.assertEquals(externalStorage,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath(externalStorage + "/Music"));
    }

    @Test
    public void getMatchedAllowedParentReturnsFilesDirForUnrelatedPath() {
        Assert.assertEquals(TermuxConstants.TERMUX_FILES_DIR_PATH,
            TermuxFileUtils.getMatchedAllowedTermuxWorkingDirectoryParentPathForPath("/opt/something"));
    }

    @Test
    public void isTermuxPrefixDirectoryAccessibleReturnsErrorWhenMissing() {
        Error error = TermuxFileUtils.isTermuxPrefixDirectoryAccessible(false, false);
        Assert.assertNotNull(error);
    }

    @Test
    public void isTermuxPrefixStagingDirectoryAccessibleReturnsErrorWhenMissing() {
        Error error = TermuxFileUtils.isTermuxPrefixStagingDirectoryAccessible(false, false);
        Assert.assertNotNull(error);
    }

    @Test
    public void isAppsTermuxAppDirectoryAccessibleReturnsErrorWhenMissing() {
        Error error = TermuxFileUtils.isAppsTermuxAppDirectoryAccessible(false, false);
        Assert.assertNotNull(error);
    }

    @Test
    public void isTermuxPrefixDirectoryEmptyReturnsFalseWhenMissing() {
        Assert.assertFalse(TermuxFileUtils.isTermuxPrefixDirectoryEmpty());
    }
}
