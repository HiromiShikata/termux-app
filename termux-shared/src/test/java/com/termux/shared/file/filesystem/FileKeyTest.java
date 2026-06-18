package com.termux.shared.file.filesystem;

import org.junit.Assert;
import org.junit.Test;

public class FileKeyTest {

    @Test
    public void equalsIsReflexive() {
        FileKey key = new FileKey(10L, 20L);
        Assert.assertEquals(key, key);
    }

    @Test
    public void equalKeysHaveSameDeviceAndInode() {
        FileKey first = new FileKey(10L, 20L);
        FileKey second = new FileKey(10L, 20L);
        Assert.assertEquals(first, second);
        Assert.assertEquals(second, first);
    }

    @Test
    public void equalKeysHaveEqualHashCodes() {
        FileKey first = new FileKey(123L, 456L);
        FileKey second = new FileKey(123L, 456L);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void keysWithDifferentDeviceAreNotEqual() {
        Assert.assertNotEquals(new FileKey(1L, 20L), new FileKey(2L, 20L));
    }

    @Test
    public void keysWithDifferentInodeAreNotEqual() {
        Assert.assertNotEquals(new FileKey(10L, 1L), new FileKey(10L, 2L));
    }

    @Test
    public void keyIsNotEqualToNull() {
        Assert.assertNotEquals(null, new FileKey(10L, 20L));
    }

    @Test
    public void keyIsNotEqualToObjectOfDifferentType() {
        Assert.assertNotEquals(new FileKey(10L, 20L), "10:20");
    }

    @Test
    public void hashCodeFoldsHighAndLowBitsOfLargeValues() {
        long highDevice = 0x1_0000_0001L;
        long highInode = 0x2_0000_0002L;
        FileKey first = new FileKey(highDevice, highInode);
        FileKey second = new FileKey(highDevice, highInode);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void toStringRendersDeviceInHexAndInodeInDecimal() {
        Assert.assertEquals("(dev=ff,ino=42)", new FileKey(255L, 42L).toString());
    }

    @Test
    public void toStringRendersZeroValues() {
        Assert.assertEquals("(dev=0,ino=0)", new FileKey(0L, 0L).toString());
    }
}
