package com.termux.shared.file.filesystem;

import org.junit.Assert;
import org.junit.Test;

public class FileTypesTest {

    @Test
    public void normalFlagsCombineRegularDirectoryAndSymlink() {
        int expected = FileType.REGULAR.getValue() | FileType.DIRECTORY.getValue() | FileType.SYMLINK.getValue();
        Assert.assertEquals(expected, FileTypes.FILE_TYPE_NORMAL_FLAGS);
        Assert.assertEquals(7, FileTypes.FILE_TYPE_NORMAL_FLAGS);
    }

    @Test
    public void anyFlagsIsIntegerMaxValue() {
        Assert.assertEquals(Integer.MAX_VALUE, FileTypes.FILE_TYPE_ANY_FLAGS);
    }

    @Test
    public void convertSingleFlagReturnsSingleName() {
        Assert.assertEquals("regular", FileTypes.convertFileTypeFlagsToNamesString(FileType.REGULAR.getValue()));
        Assert.assertEquals("directory", FileTypes.convertFileTypeFlagsToNamesString(FileType.DIRECTORY.getValue()));
    }

    @Test
    public void convertMultipleFlagsReturnsCommaSeparatedNamesInDeclaredOrder() {
        int flags = FileType.REGULAR.getValue() | FileType.DIRECTORY.getValue() | FileType.SYMLINK.getValue();
        Assert.assertEquals("regular,directory,symlink", FileTypes.convertFileTypeFlagsToNamesString(flags));
    }

    @Test
    public void convertNonContiguousFlagsKeepsDeclaredOrder() {
        int flags = FileType.REGULAR.getValue() | FileType.BLOCK.getValue();
        Assert.assertEquals("regular,block", FileTypes.convertFileTypeFlagsToNamesString(flags));
    }

    @Test
    public void convertZeroFlagsReturnsEmptyString() {
        Assert.assertEquals("", FileTypes.convertFileTypeFlagsToNamesString(0));
    }

    @Test
    public void convertAnyFlagsListsAllRecognisedTypes() {
        Assert.assertEquals("regular,directory,symlink,character,fifo,block,unknown",
            FileTypes.convertFileTypeFlagsToNamesString(FileTypes.FILE_TYPE_ANY_FLAGS));
    }

    @Test
    public void convertSocketFlagAloneReturnsEmptyStringBecauseSocketIsNotEnumerated() {
        Assert.assertEquals("", FileTypes.convertFileTypeFlagsToNamesString(FileType.SOCKET.getValue()));
    }

    @Test
    public void convertDropsSocketButKeepsEnumeratedNeighbours() {
        int flags = FileType.SYMLINK.getValue() | FileType.SOCKET.getValue() | FileType.CHARACTER.getValue();
        Assert.assertEquals("symlink,character", FileTypes.convertFileTypeFlagsToNamesString(flags));
    }

    @Test
    public void convertNoExistFlagReturnsEmptyStringBecauseValueIsZero() {
        Assert.assertEquals("", FileTypes.convertFileTypeFlagsToNamesString(FileType.NO_EXIST.getValue()));
    }

    @Test
    public void convertLastEnumeratedFlagAloneReturnsUnknown() {
        Assert.assertEquals("unknown", FileTypes.convertFileTypeFlagsToNamesString(FileType.UNKNOWN.getValue()));
    }

    @Test
    public void convertNormalFlagsConstantReturnsRegularDirectorySymlink() {
        Assert.assertEquals("regular,directory,symlink",
            FileTypes.convertFileTypeFlagsToNamesString(FileTypes.FILE_TYPE_NORMAL_FLAGS));
    }
}
