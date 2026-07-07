package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

public final class ApkUpdateAvailability {

    private final boolean updateAvailable;
    private final String latestVersionName;
    private final String downloadUrl;
    private final String assetName;
    private final long expectedSizeBytes;
    private final String downloadedFilePath;

    private ApkUpdateAvailability(boolean updateAvailable, String latestVersionName,
                                  @Nullable String downloadUrl, @Nullable String assetName,
                                  long expectedSizeBytes, @Nullable String downloadedFilePath) {
        this.updateAvailable = updateAvailable;
        this.latestVersionName = latestVersionName;
        this.downloadUrl = downloadUrl;
        this.assetName = assetName;
        this.expectedSizeBytes = expectedSizeBytes;
        this.downloadedFilePath = downloadedFilePath;
    }

    public static ApkUpdateAvailability available(String latestVersionName, String downloadUrl, String assetName) {
        return available(latestVersionName, downloadUrl, assetName, 0L);
    }

    public static ApkUpdateAvailability available(String latestVersionName, String downloadUrl, String assetName,
                                                  long expectedSizeBytes) {
        return new ApkUpdateAvailability(true, latestVersionName, downloadUrl, assetName, expectedSizeBytes, null);
    }

    public static ApkUpdateAvailability upToDate(String latestVersionName) {
        return new ApkUpdateAvailability(false, latestVersionName, null, null, 0L, null);
    }

    public ApkUpdateAvailability withDownloadedFilePath(@Nullable String filePath) {
        return new ApkUpdateAvailability(updateAvailable, latestVersionName, downloadUrl, assetName,
            expectedSizeBytes, filePath);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersionName() {
        return latestVersionName;
    }

    @Nullable
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Nullable
    public String getAssetName() {
        return assetName;
    }

    public long getExpectedSizeBytes() {
        return expectedSizeBytes;
    }

    @Nullable
    public String getDownloadedFilePath() {
        return downloadedFilePath;
    }

    public boolean hasDownloadedFilePath() {
        return downloadedFilePath != null;
    }
}
