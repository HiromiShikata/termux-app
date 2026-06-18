package com.termux.shared.file.filesystem;

import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

public class FilePermissionsTest {

    @Test
    public void toStringRendersFullPermissionSetAsRwxString() {
        Set<FilePermission> perms = EnumSet.allOf(FilePermission.class);
        Assert.assertEquals("rwxrwxrwx", FilePermissions.toString(perms));
    }

    @Test
    public void toStringRendersEmptySetAsAllDashes() {
        Assert.assertEquals("---------", FilePermissions.toString(EnumSet.noneOf(FilePermission.class)));
    }

    @Test
    public void toStringRendersOwnerOnlyPermissions() {
        Set<FilePermission> perms = EnumSet.of(FilePermission.OWNER_READ,
            FilePermission.OWNER_WRITE, FilePermission.OWNER_EXECUTE);
        Assert.assertEquals("rwx------", FilePermissions.toString(perms));
    }

    @Test
    public void toStringRendersTypicalSharedExecutablePermissions() {
        Set<FilePermission> perms = EnumSet.of(FilePermission.OWNER_READ,
            FilePermission.OWNER_WRITE, FilePermission.OWNER_EXECUTE,
            FilePermission.GROUP_READ, FilePermission.GROUP_EXECUTE);
        Assert.assertEquals("rwxr-x---", FilePermissions.toString(perms));
    }

    @Test
    public void toStringRendersOthersBitsIndependently() {
        Set<FilePermission> perms = EnumSet.of(FilePermission.OTHERS_READ,
            FilePermission.OTHERS_WRITE, FilePermission.OTHERS_EXECUTE);
        Assert.assertEquals("------rwx", FilePermissions.toString(perms));
    }

    @Test
    public void fromStringParsesFullPermissionString() {
        Set<FilePermission> result = FilePermissions.fromString("rwxrwxrwx");
        Assert.assertEquals(EnumSet.allOf(FilePermission.class), result);
    }

    @Test
    public void fromStringParsesAllDashesAsEmptySet() {
        Assert.assertTrue(FilePermissions.fromString("---------").isEmpty());
    }

    @Test
    public void fromStringParsesMixedPermissionString() {
        Set<FilePermission> result = FilePermissions.fromString("rwxr-x---");
        Assert.assertEquals(EnumSet.of(FilePermission.OWNER_READ, FilePermission.OWNER_WRITE,
            FilePermission.OWNER_EXECUTE, FilePermission.GROUP_READ, FilePermission.GROUP_EXECUTE), result);
    }

    @Test
    public void fromStringMapsEachPositionToTheCorrectPermission() {
        Assert.assertEquals(EnumSet.of(FilePermission.OWNER_READ), FilePermissions.fromString("r--------"));
        Assert.assertEquals(EnumSet.of(FilePermission.OWNER_WRITE), FilePermissions.fromString("-w-------"));
        Assert.assertEquals(EnumSet.of(FilePermission.OWNER_EXECUTE), FilePermissions.fromString("--x------"));
        Assert.assertEquals(EnumSet.of(FilePermission.GROUP_READ), FilePermissions.fromString("---r-----"));
        Assert.assertEquals(EnumSet.of(FilePermission.GROUP_WRITE), FilePermissions.fromString("----w----"));
        Assert.assertEquals(EnumSet.of(FilePermission.GROUP_EXECUTE), FilePermissions.fromString("-----x---"));
        Assert.assertEquals(EnumSet.of(FilePermission.OTHERS_READ), FilePermissions.fromString("------r--"));
        Assert.assertEquals(EnumSet.of(FilePermission.OTHERS_WRITE), FilePermissions.fromString("-------w-"));
        Assert.assertEquals(EnumSet.of(FilePermission.OTHERS_EXECUTE), FilePermissions.fromString("--------x"));
    }

    @Test
    public void toStringAndFromStringRoundTrip() {
        String permString = "rw-r--r--";
        Assert.assertEquals(permString, FilePermissions.toString(FilePermissions.fromString(permString)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsTooShortString() {
        FilePermissions.fromString("rwxr-x--");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsTooLongString() {
        FilePermissions.fromString("rwxr-x----");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsInvalidReadCharacter() {
        FilePermissions.fromString("Rwxrwxrwx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsInvalidWriteCharacter() {
        FilePermissions.fromString("rWxrwxrwx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsInvalidExecuteCharacter() {
        FilePermissions.fromString("rwXrwxrwx");
    }
}
