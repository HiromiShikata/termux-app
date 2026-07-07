package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ApkUpdateCachedFileResolverTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ApkUpdateCachedFileResolver resolver = new ApkUpdateCachedFileResolver();

    @Test
    public void returnsFileWhenPersistedPathExistsAndIsValid() throws IOException {
        File apkFile = writeValidApk("termux-app_arm64-v8a.apk");
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

    @Test
    public void deletesAndReturnsNullWhenCachedFileIsCorrupt() throws IOException {
        File corruptFile = temporaryFolder.newFile("termux-app_arm64-v8a.apk");
        try (OutputStream outputStream = new FileOutputStream(corruptFile)) {
            outputStream.write(new byte[]{0x00, 0x01, 0x02, 0x03});
        }
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.121.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath(corruptFile.getAbsolutePath());

        File resolved = resolver.resolveExistingFile(availability);

        Assert.assertNull(resolved);
        Assert.assertFalse(corruptFile.exists());
    }

    private File writeValidApk(String fileName) throws IOException {
        File apkFile = temporaryFolder.newFile(fileName);
        try (OutputStream outputStream = new FileOutputStream(apkFile)) {
            outputStream.write(new byte[]{0x50, 0x4B, 0x03, 0x04});
            outputStream.write(new byte[2 * 1024 * 1024]);
        }
        return apkFile;
    }
}
