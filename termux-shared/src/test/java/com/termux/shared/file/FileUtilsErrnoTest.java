package com.termux.shared.file;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilsErrnoTest {

    @Test
    public void typeConstantIsFileUtilsError() {
        Assert.assertEquals("FileUtils Error", FileUtilsErrno.TYPE);
    }

    @Test
    public void executableRequiredErrnoExposesCodeAndMessage() {
        Assert.assertEquals(100, FileUtilsErrno.ERRNO_EXECUTABLE_REQUIRED.getCode());
        Assert.assertEquals("Executable required.", FileUtilsErrno.ERRNO_EXECUTABLE_REQUIRED.getMessage());
        Assert.assertEquals(FileUtilsErrno.TYPE, FileUtilsErrno.ERRNO_EXECUTABLE_REQUIRED.getType());
    }

    @Test
    public void registeredErrnoIsResolvableByTypeAndCode() {
        Errno resolved = Errno.valueOf(FileUtilsErrno.TYPE, 100);
        Assert.assertSame(FileUtilsErrno.ERRNO_EXECUTABLE_REQUIRED, resolved);
    }

    @Test
    public void fileNotFoundErrnoFormatsPlaceholdersWithArgs() {
        Error error = FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH.getError("regular file", "/tmp/x");
        Assert.assertEquals("The regular file not found at path \"/tmp/x\".", error.getMessage());
    }

    @Test
    public void getErrorPreservesFileUtilsTypeAndCode() {
        Error error = FileUtilsErrno.ERRNO_NULL_OR_EMPTY_REGULAR_FILE_PATH.getError();
        Assert.assertEquals(FileUtilsErrno.TYPE, error.getType());
        Assert.assertEquals(Integer.valueOf(101), error.getCode());
    }

    @Test
    public void equalsErrorTypeAndCodeMatchesGeneratedError() {
        Error error = FileUtilsErrno.ERRNO_NON_REGULAR_FILE_FOUND.getError("a", "b");
        Assert.assertTrue(FileUtilsErrno.ERRNO_NON_REGULAR_FILE_FOUND.equalsErrorTypeAndCode(error));
    }

    @Test
    public void shortMappingMapsFullErrnoToShortVariant() {
        Assert.assertSame(FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH_SHORT,
            FileUtilsErrno.ERRNO_SHORT_MAPPING.get(FileUtilsErrno.ERRNO_FILE_NOT_FOUND_AT_PATH));
    }

    @Test
    public void shortMappingHasNoEntryForUnmappedErrno() {
        Assert.assertNull(FileUtilsErrno.ERRNO_SHORT_MAPPING.get(FileUtilsErrno.ERRNO_EXECUTABLE_REQUIRED));
    }

    @Test
    public void distinctErrnoConstantsHaveDistinctCodes() {
        Assert.assertNotEquals(FileUtilsErrno.ERRNO_CREATING_FILE_FAILED.getCode(),
            FileUtilsErrno.ERRNO_COPYING_OR_MOVING_FILE_FAILED_WITH_EXCEPTION.getCode());
    }
}
