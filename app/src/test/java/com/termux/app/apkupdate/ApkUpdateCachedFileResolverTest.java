package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class ApkUpdateCachedFileResolverTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ApkUpdateCachedFileResolver resolver = new ApkUpdateCachedFileResolver();

    @Test
    public void returnsFileWhenPersistedPathExists() throws IOException {
        File apkFile = temporaryFolder.newFile("termux-app_arm64-v8a.apk");
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath(apkFile.getAbsolutePath());

        File resolved = resolver.resolveExistingFile(availability);

        Assert.assertNotNull(resolved);
        Assert.assertEquals(apkFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void returnsNullWhenPersistedPathDoesNotExist() {
        File missingFile = new File(temporaryFolder.getRoot(), "missing.apk");
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath(missingFile.getAbsolutePath());

        Assert.assertNull(resolver.resolveExistingFile(availability));
    }

    @Test
    public void returnsNullWhenAvailabilityHasNoDownloadedFilePath() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        Assert.assertNull(resolver.resolveExistingFile(availability));
    }

    @Test
    public void returnsNullWhenPathPointsToDirectory() throws IOException {
        File directory = temporaryFolder.newFolder("apkupdate");
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath(directory.getAbsolutePath());

        Assert.assertNull(resolver.resolveExistingFile(availability));
    }

    @Test
    public void returnsNullWhenAvailabilityIsNull() {
        Assert.assertNull(resolver.resolveExistingFile(null));
    }
}
