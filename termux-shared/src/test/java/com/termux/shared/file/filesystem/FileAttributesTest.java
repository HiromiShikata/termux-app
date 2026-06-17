package com.termux.shared.file.filesystem;

import android.system.OsConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class FileAttributesTest {

    private static FileAttributes newWithFilePath(String filePath) throws Exception {
        Constructor<FileAttributes> constructor = FileAttributes.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(filePath);
    }

    private static FileAttributes newWithFileDescriptor(FileDescriptor fileDescriptor) throws Exception {
        Constructor<FileAttributes> constructor = FileAttributes.class.getDeclaredConstructor(FileDescriptor.class);
        constructor.setAccessible(true);
        return constructor.newInstance(fileDescriptor);
    }

    private static void setLong(FileAttributes attributes, String name, long value) throws Exception {
        Field field = FileAttributes.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(attributes, value);
    }

    private static void setInt(FileAttributes attributes, String name, int value) throws Exception {
        Field field = FileAttributes.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(attributes, value);
    }

    private static FileAttributes attributesWithMode(int mode) throws Exception {
        FileAttributes attributes = newWithFilePath("/data/local/file");
        setInt(attributes, "st_mode", mode);
        setLong(attributes, "st_ino", 5L);
        setLong(attributes, "st_dev", 11L);
        setLong(attributes, "st_rdev", 19L);
        setLong(attributes, "st_nlink", 2L);
        setInt(attributes, "st_uid", 1000);
        setInt(attributes, "st_gid", 1000);
        setLong(attributes, "st_size", 4096L);
        setLong(attributes, "st_blksize", 512L);
        setLong(attributes, "st_blocks", 8L);
        setLong(attributes, "st_atime_sec", 100L);
        setLong(attributes, "st_atime_nsec", 0L);
        setLong(attributes, "st_mtime_sec", 200L);
        setLong(attributes, "st_mtime_nsec", 500000L);
        setLong(attributes, "st_ctime_sec", 300L);
        setLong(attributes, "st_ctime_nsec", 0L);
        return attributes;
    }

    @Test
    public void regularFilePredicatesAndAccessors() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG | OsConstants.S_IRUSR | OsConstants.S_IWUSR);
        Assert.assertTrue(attributes.isRegularFile());
        Assert.assertFalse(attributes.isDirectory());
        Assert.assertFalse(attributes.isSymbolicLink());
        Assert.assertFalse(attributes.isDevice());
        Assert.assertFalse(attributes.isOther());
        Assert.assertEquals("/data/local/file", attributes.file());
        Assert.assertEquals(5L, attributes.ino());
        Assert.assertEquals(11L, attributes.dev());
        Assert.assertEquals(19L, attributes.rdev());
        Assert.assertEquals(2L, attributes.nlink());
        Assert.assertEquals(1000, attributes.uid());
        Assert.assertEquals(1000, attributes.gid());
        Assert.assertEquals(4096L, attributes.size());
        Assert.assertEquals(512L, attributes.blksize());
        Assert.assertEquals(8L, attributes.blocks());
        Assert.assertEquals(OsConstants.S_IFREG | OsConstants.S_IRUSR | OsConstants.S_IWUSR, attributes.mode());
    }

    @Test
    public void directoryPredicate() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFDIR);
        Assert.assertTrue(attributes.isDirectory());
        Assert.assertFalse(attributes.isRegularFile());
        Assert.assertFalse(attributes.isOther());
    }

    @Test
    public void symbolicLinkPredicate() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFLNK);
        Assert.assertTrue(attributes.isSymbolicLink());
        Assert.assertFalse(attributes.isOther());
    }

    @Test
    public void otherTypePredicateForSocket() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFSOCK);
        Assert.assertTrue(attributes.isOther());
    }

    @Test
    public void characterDevicePredicates() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFCHR);
        Assert.assertTrue(attributes.isCharacter());
        Assert.assertTrue(attributes.isDevice());
    }

    @Test
    public void blockDevicePredicates() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFBLK);
        Assert.assertTrue(attributes.isBlock());
        Assert.assertTrue(attributes.isDevice());
    }

    @Test
    public void fifoPredicates() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFIFO);
        Assert.assertTrue(attributes.isFifo());
        Assert.assertTrue(attributes.isDevice());
    }

    @Test
    public void socketPredicate() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFSOCK);
        Assert.assertTrue(attributes.isSocket());
    }

    @Test
    public void permissionsReturnsNonNullSet() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG);
        Set<FilePermission> permissions = attributes.permissions();
        Assert.assertNotNull(permissions);
    }

    @Test
    public void timeConversionHandlesZeroAndNonZeroNanoseconds() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG);
        Assert.assertNotNull(attributes.lastAccessTime());
        Assert.assertNotNull(attributes.lastModifiedTime());
        Assert.assertNotNull(attributes.lastChangeTime());
        Assert.assertEquals(attributes.lastModifiedTime().toMillis(), attributes.creationTime().toMillis());
    }

    @Test
    public void ownerGroupAndFileKeyAreCachedAndConsistent() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG);
        Assert.assertEquals("1000", attributes.owner());
        Assert.assertEquals("1000", attributes.owner());
        Assert.assertEquals("1000", attributes.group());
        FileKey firstKey = attributes.fileKey();
        Assert.assertSame(firstKey, attributes.fileKey());
    }

    @Test
    public void isSameFileComparesInodeAndDevice() throws Exception {
        FileAttributes first = attributesWithMode(OsConstants.S_IFREG);
        FileAttributes second = attributesWithMode(OsConstants.S_IFREG);
        Assert.assertTrue(first.isSameFile(second));
        setLong(second, "st_ino", 999L);
        Assert.assertFalse(first.isSameFile(second));
    }

    @Test
    public void fileReturnsDescriptorStringWhenNoPathAndNullWhenNeither() throws Exception {
        FileAttributes withDescriptor = newWithFileDescriptor(new FileDescriptor());
        Assert.assertNotNull(withDescriptor.file());

        FileAttributes empty = newWithFilePath(null);
        Assert.assertNull(empty.file());
    }

    @Test
    public void logStringContainsAllSections() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG | OsConstants.S_IRUSR);
        String logString = attributes.toString();
        Assert.assertTrue(logString.contains("File:"));
        Assert.assertTrue(logString.contains("Type:"));
        Assert.assertTrue(logString.contains("Size:"));
        Assert.assertTrue(logString.contains("Owner:"));
        Assert.assertTrue(logString.contains("Permissions:"));
        Assert.assertTrue(logString.contains("Access Time:"));
        Assert.assertFalse(logString.contains("Device Type:"));
    }

    @Test
    public void logStringIncludesDeviceTypeForCharacterDevice() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFCHR);
        String logString = FileAttributes.getFileAttributesLogString(attributes);
        Assert.assertTrue(logString.contains("Device Type:"));
    }

    @Test
    public void logStringForNullReturnsNullLiteral() {
        Assert.assertEquals("null", FileAttributes.getFileAttributesLogString(null));
    }

    @Test
    public void individualStringGettersExposeValues() throws Exception {
        FileAttributes attributes = attributesWithMode(OsConstants.S_IFREG);
        Assert.assertTrue(attributes.getFileString().contains("/data/local/file"));
        Assert.assertTrue(attributes.getSizeString().contains("4096"));
        Assert.assertTrue(attributes.getBlocksString().contains("8"));
        Assert.assertTrue(attributes.getIOBlockString().contains("512"));
        Assert.assertTrue(attributes.getInodeString().contains("5"));
        Assert.assertTrue(attributes.getLinksString().contains("2"));
        Assert.assertTrue(attributes.getDeviceTypeString().contains("19"));
        Assert.assertTrue(attributes.getOwnerString().contains("1000"));
        Assert.assertTrue(attributes.getGroupString().contains("1000"));
        Assert.assertTrue(attributes.getDeviceString().contains(Long.toHexString(11L)));
    }
}
