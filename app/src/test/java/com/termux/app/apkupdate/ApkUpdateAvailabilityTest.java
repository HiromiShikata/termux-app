package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ApkUpdateAvailabilityTest {

    @Test
    public void availableCarriesVersionDownloadUrlAndAssetName() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        Assert.assertTrue(availability.isUpdateAvailable());
        Assert.assertEquals("0.119.0", availability.getLatestVersionName());
        Assert.assertEquals("https://example.com/arm64", availability.getDownloadUrl());
        Assert.assertEquals("termux-app_arm64-v8a.apk", availability.getAssetName());
    }

    @Test
    public void upToDateReportsNoUpdateAndNullDownloadDetails() {
        ApkUpdateAvailability availability = ApkUpdateAvailability.upToDate("0.118.0");

        Assert.assertFalse(availability.isUpdateAvailable());
        Assert.assertEquals("0.118.0", availability.getLatestVersionName());
        Assert.assertNull(availability.getDownloadUrl());
        Assert.assertNull(availability.getAssetName());
    }

    @Test
    public void availableHasNoDownloadedFilePathByDefault() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        Assert.assertFalse(availability.hasDownloadedFilePath());
        Assert.assertNull(availability.getDownloadedFilePath());
    }

    @Test
    public void withDownloadedFilePathCarriesPathAndPreservesOtherFields() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath("/data/cache/apkupdate/termux-app_arm64-v8a.apk");

        Assert.assertTrue(availability.hasDownloadedFilePath());
        Assert.assertEquals("/data/cache/apkupdate/termux-app_arm64-v8a.apk",
            availability.getDownloadedFilePath());
        Assert.assertTrue(availability.isUpdateAvailable());
        Assert.assertEquals("0.119.0", availability.getLatestVersionName());
        Assert.assertEquals("https://example.com/arm64", availability.getDownloadUrl());
        Assert.assertEquals("termux-app_arm64-v8a.apk", availability.getAssetName());
    }

    @Test
    public void withDownloadedFilePathNullClearsPath() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk")
                .withDownloadedFilePath("/data/cache/apkupdate/termux-app_arm64-v8a.apk")
                .withDownloadedFilePath(null);

        Assert.assertFalse(availability.hasDownloadedFilePath());
        Assert.assertNull(availability.getDownloadedFilePath());
    }
}
