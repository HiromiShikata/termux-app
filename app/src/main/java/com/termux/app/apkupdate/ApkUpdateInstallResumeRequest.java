package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

public final class ApkUpdateInstallResumeRequest {

    static final String KEY_VERSION_NAME = "apk_update_resume_install_version_name";

    static final String KEY_DOWNLOAD_URL = "apk_update_resume_install_download_url";

    static final String KEY_ASSET_NAME = "apk_update_resume_install_asset_name";

    static final String KEY_EXPECTED_SIZE_BYTES = "apk_update_resume_install_expected_size_bytes";

    static final String KEY_DOWNLOADED_FILE_PATH = "apk_update_resume_install_downloaded_file_path";

    private final ApkUpdatePendingState.Store store;

    public ApkUpdateInstallResumeRequest(ApkUpdatePendingState.Store store) {
        this.store = store;
    }

    public void saveResumeRequest(ApkUpdateAvailability availability) {
        if (!availability.isUpdateAvailable()) {
            clearResumeRequest();
            return;
        }
        store.putString(KEY_VERSION_NAME, availability.getLatestVersionName());
        store.putString(KEY_DOWNLOAD_URL, availability.getDownloadUrl());
        store.putString(KEY_ASSET_NAME, availability.getAssetName());
        store.putString(KEY_EXPECTED_SIZE_BYTES, Long.toString(availability.getExpectedSizeBytes()));
        if (availability.hasDownloadedFilePath()) {
            store.putString(KEY_DOWNLOADED_FILE_PATH, availability.getDownloadedFilePath());
        } else {
            store.remove(KEY_DOWNLOADED_FILE_PATH);
        }
    }

    @Nullable
    public ApkUpdateAvailability loadResumeRequest() {
        String versionName = store.getString(KEY_VERSION_NAME);
        String downloadUrl = store.getString(KEY_DOWNLOAD_URL);
        String assetName = store.getString(KEY_ASSET_NAME);
        if (versionName == null || downloadUrl == null || assetName == null) {
            return null;
        }
        long expectedSizeBytes = parseExpectedSizeBytes(store.getString(KEY_EXPECTED_SIZE_BYTES));
        String downloadedFilePath = store.getString(KEY_DOWNLOADED_FILE_PATH);
        return ApkUpdateAvailability.available(versionName, downloadUrl, assetName, expectedSizeBytes)
            .withDownloadedFilePath(downloadedFilePath);
    }

    public boolean hasResumeRequest() {
        return loadResumeRequest() != null;
    }

    public void clearResumeRequest() {
        store.remove(KEY_VERSION_NAME);
        store.remove(KEY_DOWNLOAD_URL);
        store.remove(KEY_ASSET_NAME);
        store.remove(KEY_EXPECTED_SIZE_BYTES);
        store.remove(KEY_DOWNLOADED_FILE_PATH);
    }

    private long parseExpectedSizeBytes(@Nullable String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
