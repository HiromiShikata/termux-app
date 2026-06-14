package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

public final class ApkUpdateAvailability {

    private final boolean updateAvailable;
    private final String latestVersionName;
    private final String downloadUrl;
    private final String assetName;

    private ApkUpdateAvailability(boolean updateAvailable, String latestVersionName,
                                  @Nullable String downloadUrl, @Nullable String assetName) {
        this.updateAvailable = updateAvailable;
        this.latestVersionName = latestVersionName;
        this.downloadUrl = downloadUrl;
        this.assetName = assetName;
    }

    public static ApkUpdateAvailability available(String latestVersionName, String downloadUrl, String assetName) {
        return new ApkUpdateAvailability(true, latestVersionName, downloadUrl, assetName);
    }

    public static ApkUpdateAvailability upToDate(String latestVersionName) {
        return new ApkUpdateAvailability(false, latestVersionName, null, null);
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
}
