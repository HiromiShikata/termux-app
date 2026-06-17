package com.termux.shared.file;

import com.termux.shared.errors.Error;
import com.termux.shared.errors.FunctionErrno;
import com.termux.shared.file.filesystem.FileType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class FileUtilsTest {

    private static File newTempDir() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"),
            "futest_" + System.nanoTime());
        Assert.assertTrue(dir.mkdirs());
        dir.deleteOnExit();
        return dir;
    }

    @Test
    public void getCanonicalPathResolvesRelativeSegmentsForAbsolutePath() {
        Assert.assertEquals("/a/c", FileUtils.getCanonicalPath("/a/b/../c", null));
    }

    @Test
    public void getCanonicalPathPrefixesNonAbsolutePathWithProvidedPrefix() {
        Assert.assertEquals("/base/rel", FileUtils.getCanonicalPath("rel", "/base"));
    }

    @Test
    public void getCanonicalPathPrefixesNonAbsolutePathWithSlashWhenPrefixNull() {
        Assert.assertEquals("/rel", FileUtils.getCanonicalPath("rel", null));
    }

    @Test
    public void getCanonicalPathTreatsNullPathAsEmpty() {
        Assert.assertEquals("/base", FileUtils.getCanonicalPath(null, "/base"));
    }

    @Test
    public void normalizePathReturnsNullForNullInput() {
        Assert.assertNull(FileUtils.normalizePath(null));
    }

    @Test
    public void normalizePathCollapsesRepeatedSlashes() {
        Assert.assertEquals("/a/b/c", FileUtils.normalizePath("/a//b///c"));
    }

    @Test
    public void normalizePathRemovesTrailingSlash() {
        Assert.assertEquals("/a/b", FileUtils.normalizePath("/a/b/"));
    }

    @Test
    public void normalizePathRemovesDotSlashSegments() {
        Assert.assertEquals("a/b", FileUtils.normalizePath("a/./b"));
    }

    @Test
    public void sanitizeFileNameReturnsNullForNullInput() {
        Assert.assertNull(FileUtils.sanitizeFileName(null, true, true));
    }

    @Test
    public void sanitizeFileNameReplacesSpecialCharactersWithUnderscore() {
        Assert.assertEquals("a_b_c__", FileUtils.sanitizeFileName("a/b:c*?", false, false));
    }

    @Test
    public void sanitizeFileNameReplacesWhitespaceWhenRequested() {
        Assert.assertEquals("A_B_C", FileUtils.sanitizeFileName("A B\tC", true, false));
    }

    @Test
    public void sanitizeFileNameKeepsWhitespaceWhenNotRequested() {
        Assert.assertEquals("A B C", FileUtils.sanitizeFileName("A B C", false, false));
    }

    @Test
    public void sanitizeFileNameLowercasesWhenRequested() {
        Assert.assertEquals("abc", FileUtils.sanitizeFileName("ABC", false, true));
    }

    @Test
    public void isPathInDirPathDetectsPathUnderDirectory() {
        Assert.assertTrue(FileUtils.isPathInDirPath("/a/b/c", "/a/b", true));
    }

    @Test
    public void isPathInDirPathRejectsEqualPathWhenEnsureUnder() {
        Assert.assertFalse(FileUtils.isPathInDirPath("/a/b", "/a/b", true));
    }

    @Test
    public void isPathInDirPathRejectsEqualPathWhenNotEnsureUnder() {
        Assert.assertFalse(FileUtils.isPathInDirPath("/a/b", "/a/b", false));
    }

    @Test
    public void isPathInDirPathsReturnsFalseForNullPath() {
        Assert.assertFalse(FileUtils.isPathInDirPaths(null, Collections.singletonList("/a"), true));
    }

    @Test
    public void isPathInDirPathsReturnsFalseForEmptyDirPaths() {
        Assert.assertFalse(FileUtils.isPathInDirPaths("/a/b", Collections.emptyList(), true));
    }

    @Test
    public void isPathInDirPathsMatchesOneOfMultipleDirectories() {
        Assert.assertTrue(FileUtils.isPathInDirPaths("/x/y/z", Arrays.asList("/a/b", "/x/y"), true));
    }

    @Test
    public void isValidPermissionStringAcceptsCanonicalForms() {
        Assert.assertTrue(FileUtils.isValidPermissionString("rwx"));
        Assert.assertTrue(FileUtils.isValidPermissionString("r-x"));
        Assert.assertTrue(FileUtils.isValidPermissionString("---"));
    }

    @Test
    public void isValidPermissionStringRejectsInvalidForms() {
        Assert.assertFalse(FileUtils.isValidPermissionString("rw"));
        Assert.assertFalse(FileUtils.isValidPermissionString("rwxx"));
        Assert.assertFalse(FileUtils.isValidPermissionString("xwr"));
        Assert.assertFalse(FileUtils.isValidPermissionString("Rwx"));
        Assert.assertFalse(FileUtils.isValidPermissionString(""));
        Assert.assertFalse(FileUtils.isValidPermissionString(null));
    }

    @Test
    public void getFileDirnameReturnsParentDirectory() {
        Assert.assertEquals("/a/b", FileUtils.getFileDirname("/a/b/c.txt"));
    }

    @Test
    public void getFileDirnameReturnsNullWhenNoSlash() {
        Assert.assertNull(FileUtils.getFileDirname("noslash"));
    }

    @Test
    public void getFileDirnameReturnsNullForEmpty() {
        Assert.assertNull(FileUtils.getFileDirname(""));
    }

    @Test
    public void getFileBasenameReturnsLastSegment() {
        Assert.assertEquals("c.txt", FileUtils.getFileBasename("/a/b/c.txt"));
    }

    @Test
    public void getFileBasenameReturnsWholeStringWhenNoSlash() {
        Assert.assertEquals("noslash", FileUtils.getFileBasename("noslash"));
    }

    @Test
    public void getFileBasenameReturnsNullForNull() {
        Assert.assertNull(FileUtils.getFileBasename(null));
    }

    @Test
    public void getFileBasenameWithoutExtensionStripsExtension() {
        Assert.assertEquals("c", FileUtils.getFileBasenameWithoutExtension("/a/b/c.txt"));
    }

    @Test
    public void getFileBasenameWithoutExtensionKeepsNameWhenNoExtension() {
        Assert.assertEquals("c", FileUtils.getFileBasenameWithoutExtension("/a/b/c"));
    }

    @Test
    public void getFileBasenameWithoutExtensionReturnsNullForNull() {
        Assert.assertNull(FileUtils.getFileBasenameWithoutExtension(null));
    }

    @Test
    public void isCharsetSupportedReturnsNullForSupportedCharset() {
        Assert.assertNull(FileUtils.isCharsetSupported(StandardCharsets.UTF_8));
    }

    @Test
    public void isCharsetSupportedReturnsErrorForNullCharset() {
        Error error = FileUtils.isCharsetSupported(null);
        Assert.assertNotNull(error);
        Assert.assertEquals(FunctionErrno.TYPE, error.getType());
    }

    @Test
    public void closeCloseableIgnoresNull() {
        FileUtils.closeCloseable(null);
    }

    @Test
    public void closeCloseableClosesObject() {
        final boolean[] closed = {false};
        FileUtils.closeCloseable(new Closeable() {
            @Override
            public void close() {
                closed[0] = true;
            }
        });
        Assert.assertTrue(closed[0]);
    }

    @Test
    public void closeCloseableSwallowsExceptions() {
        FileUtils.closeCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        });
    }

    @Test
    public void getFileTypeReturnsNoExistForNullOrEmptyPath() {
        Assert.assertEquals(FileType.NO_EXIST, FileUtils.getFileType(null, false));
        Assert.assertEquals(FileType.NO_EXIST, FileUtils.getFileType("", false));
    }

    @Test
    public void regularFileExistsReturnsFalseForMissingFile() {
        Assert.assertFalse(FileUtils.regularFileExists("/this/does/not/exist/abc.xyz", false));
    }

    @Test
    public void directoryFileExistsDetectsRealDirectory() throws IOException {
        File dir = newTempDir();
        Assert.assertTrue(FileUtils.directoryFileExists(dir.getAbsolutePath(), false));
        Assert.assertTrue(FileUtils.fileExists(dir.getAbsolutePath(), false));
        Assert.assertTrue(FileUtils.regularOrDirectoryFileExists(dir.getAbsolutePath(), false));
    }

    @Test
    public void checkMissingFilePermissionsReturnsNullForReadableFile() throws IOException {
        File dir = newTempDir();
        File file = new File(dir, "readable.txt");
        Assert.assertTrue(file.createNewFile());
        file.deleteOnExit();
        Assert.assertNull(FileUtils.checkMissingFilePermissions("label", file.getAbsolutePath(), "r--", true));
    }

    @Test
    public void checkMissingFilePermissionsReturnsErrorForMissingPath() {
        Error error = FileUtils.checkMissingFilePermissions(null, "/no/such/file/at/all.xyz", "r--", true);
        Assert.assertNotNull(error);
        Assert.assertEquals(FileUtilsErrno.TYPE, error.getType());
    }

    @Test
    public void checkMissingFilePermissionsReturnsErrorForEmptyPath() {
        Error error = FileUtils.checkMissingFilePermissions(null, "", "r--", true);
        Assert.assertNotNull(error);
        Assert.assertEquals(FunctionErrno.TYPE, error.getType());
    }

    @Test
    public void checkMissingFilePermissionsRejectsInvalidPermissionString() {
        Error error = FileUtils.checkMissingFilePermissions("label", "/tmp/whatever", "invalid", true);
        Assert.assertNotNull(error);
        Assert.assertEquals(FileUtilsErrno.ERRNO_INVALID_FILE_PERMISSIONS_STRING_TO_CHECK.getCode(), error.getCode().intValue());
    }

    @Test
    public void setFilePermissionsIgnoresNullPath() {
        FileUtils.setFilePermissions(null, "rwx");
    }

    @Test
    public void setFilePermissionsIgnoresInvalidPermissionString() {
        FileUtils.setFilePermissions("/tmp/whatever", "invalid");
    }

    @Test
    public void setMissingFilePermissionsIgnoresNullPath() {
        FileUtils.setMissingFilePermissions(null, "rwx");
    }

    @Test
    public void setMissingFilePermissionsAppliesToRealFile() throws IOException {
        File dir = newTempDir();
        File file = new File(dir, "perm.txt");
        Assert.assertTrue(file.createNewFile());
        file.deleteOnExit();
        FileUtils.setMissingFilePermissions("label", file.getAbsolutePath(), "rw-");
        Assert.assertTrue(file.canRead());
    }

    @Test
    public void getShortFileUtilsErrorReturnsSameErrorForNonFileUtilsType() {
        Error original = FunctionErrno.ERRNO_NULL_OR_EMPTY_PARAMETER.getError("param", "fn");
        Assert.assertSame(original, FileUtils.getShortFileUtilsError(original));
    }

    @Test
    public void getShortFileUtilsErrorMapsToShortVariant() {
        Error original = FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError("file", "/a/b");
        Error shortError = FileUtils.getShortFileUtilsError(original);
        Assert.assertEquals(FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH_SHORT.getCode(), shortError.getCode().intValue());
    }

    @Test
    public void executablePermissionConstantsAreStable() {
        Assert.assertEquals("r-x", FileUtils.APP_EXECUTABLE_FILE_PERMISSIONS);
        Assert.assertEquals("rwx", FileUtils.APP_WORKING_DIRECTORY_PERMISSIONS);
    }
}
