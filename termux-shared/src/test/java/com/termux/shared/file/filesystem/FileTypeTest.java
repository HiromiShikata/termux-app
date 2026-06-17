package com.termux.shared.file.filesystem;

import org.junit.Assert;
import org.junit.Test;

public class FileTypeTest {

    @Test
    public void eachFileTypeExposesItsNameAndBitValue() {
        Assert.assertEquals("no exist", FileType.NO_EXIST.getName());
        Assert.assertEquals(0, FileType.NO_EXIST.getValue());
        Assert.assertEquals("regular", FileType.REGULAR.getName());
        Assert.assertEquals(1, FileType.REGULAR.getValue());
        Assert.assertEquals("directory", FileType.DIRECTORY.getName());
        Assert.assertEquals(2, FileType.DIRECTORY.getValue());
        Assert.assertEquals("symlink", FileType.SYMLINK.getName());
        Assert.assertEquals(4, FileType.SYMLINK.getValue());
        Assert.assertEquals("socket", FileType.SOCKET.getName());
        Assert.assertEquals(8, FileType.SOCKET.getValue());
        Assert.assertEquals("character", FileType.CHARACTER.getName());
        Assert.assertEquals(16, FileType.CHARACTER.getValue());
        Assert.assertEquals("fifo", FileType.FIFO.getName());
        Assert.assertEquals(32, FileType.FIFO.getValue());
        Assert.assertEquals("block", FileType.BLOCK.getName());
        Assert.assertEquals(64, FileType.BLOCK.getValue());
        Assert.assertEquals("unknown", FileType.UNKNOWN.getName());
        Assert.assertEquals(128, FileType.UNKNOWN.getValue());
    }

    @Test
    public void bitValuesArePowersOfTwoAndDistinct() {
        FileType[] flagged = {FileType.REGULAR, FileType.DIRECTORY, FileType.SYMLINK, FileType.SOCKET,
            FileType.CHARACTER, FileType.FIFO, FileType.BLOCK, FileType.UNKNOWN};
        int combined = 0;
        for (FileType type : flagged) {
            Assert.assertEquals(0, combined & type.getValue());
            combined |= type.getValue();
        }
        Assert.assertEquals(255, combined);
    }

    @Test
    public void valueOfResolvesEnumConstantByName() {
        Assert.assertSame(FileType.REGULAR, FileType.valueOf("REGULAR"));
    }
}
