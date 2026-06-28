package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class ApkUpdateCachePrunerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ApkUpdateCachePruner pruner = new ApkUpdateCachePruner();

    @Test
    public void deletesPreviouslyDownloadedApksAndKeepsTheCurrentOne() throws IOException {
        File cacheDirectory = temporaryFolder.newFolder("apkupdate");
        File staleApk = new File(cacheDirectory, "termux-app_v0.118.0+old.apk");
        File anotherStaleApk = new File(cacheDirectory, "termux-app_v0.118.1+older.apk");
        File currentApk = new File(cacheDirectory, "termux-app_v0.119.0+new.apk");
        Assert.assertTrue(staleApk.createNewFile());
        Assert.assertTrue(anotherStaleApk.createNewFile());
        Assert.assertTrue(currentApk.createNewFile());

        pruner.pruneExcept(cacheDirectory, currentApk.getName());

        Assert.assertFalse(staleApk.exists());
        Assert.assertFalse(anotherStaleApk.exists());
        Assert.assertTrue(currentApk.exists());
    }

    @Test
    public void keepsTheFileToKeepWhenItIsTheOnlyFile() throws IOException {
        File cacheDirectory = temporaryFolder.newFolder("apkupdate");
        File currentApk = new File(cacheDirectory, "termux-app_v0.119.0+new.apk");
        Assert.assertTrue(currentApk.createNewFile());

        pruner.pruneExcept(cacheDirectory, currentApk.getName());

        Assert.assertTrue(currentApk.exists());
    }

    @Test
    public void leavesSubdirectoriesUntouched() throws IOException {
        File cacheDirectory = temporaryFolder.newFolder("apkupdate");
        File subdirectory = new File(cacheDirectory, "nested");
        Assert.assertTrue(subdirectory.mkdir());
        File staleApk = new File(cacheDirectory, "termux-app_v0.118.0+old.apk");
        Assert.assertTrue(staleApk.createNewFile());

        pruner.pruneExcept(cacheDirectory, "termux-app_v0.119.0+new.apk");

        Assert.assertFalse(staleApk.exists());
        Assert.assertTrue(subdirectory.isDirectory());
    }

    @Test
    public void doesNothingWhenDirectoryDoesNotExist() {
        File missingDirectory = new File(temporaryFolder.getRoot(), "missing");

        pruner.pruneExcept(missingDirectory, "termux-app_v0.119.0+new.apk");

        Assert.assertFalse(missingDirectory.exists());
    }
}
