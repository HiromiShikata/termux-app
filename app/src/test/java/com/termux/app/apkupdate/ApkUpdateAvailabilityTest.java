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
    public void availableDefaultsExpectedSizeBytesToZero() {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");

        Assert.assertEquals(0L, availability.getExpectedSizeBytes());
    }

    @Test
    public void availableCarriesExpectedSizeBytes() {
        ApkUpdateAvailability availability = ApkUpdateAvailability.available(
            "0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk", 4567890L);

        Assert.assertEquals(4567890L, availability.getExpectedSizeBytes());
    }

    @Test
    public void withDownloadedFilePathPreservesExpectedSizeBytes() {
        ApkUpdateAvailability availability = ApkUpdateAvailability.available(
                "0.119.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk", 4567890L)
            .withDownloadedFilePath("/data/cache/apkupdate/termux-app_arm64-v8a.apk");

        Assert.assertEquals(4567890L, availability.getExpectedSizeBytes());
        Assert.assertEquals("/data/cache/apkupdate/termux-app_arm64-v8a.apk",
            availability.getDownloadedFilePath());
    }

    @Test
    public void upToDateHasZeroExpectedSizeBytes() {
        ApkUpdateAvailability availability = ApkUpdateAvailability.upToDate("0.118.0");

        Assert.assertEquals(0L, availability.getExpectedSizeBytes());
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
