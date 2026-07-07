package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

public final class ApkUpdatePendingState {

    public interface Store {
        @Nullable
        String getString(String key);

        void putString(String key, String value);

        void remove(String key);
    }

    static final String KEY_LATEST_VERSION_NAME = "apk_update_pending_latest_version_name";

    static final String KEY_DOWNLOAD_URL = "apk_update_pending_download_url";

    static final String KEY_ASSET_NAME = "apk_update_pending_asset_name";

    static final String KEY_EXPECTED_SIZE_BYTES = "apk_update_pending_expected_size_bytes";

    static final String KEY_DOWNLOADED_FILE_PATH = "apk_update_pending_downloaded_file_path";

    private final Store store;
    private final AppVersionComparator versionComparator;

    public ApkUpdatePendingState(Store store) {
        this(store, new AppVersionComparator());
    }

    public ApkUpdatePendingState(Store store, AppVersionComparator versionComparator) {
        this.store = store;
        this.versionComparator = versionComparator;
    }

    public void save(ApkUpdateAvailability availability) {
        if (!availability.isUpdateAvailable()) {
            clear();
            return;
        }
        store.putString(KEY_LATEST_VERSION_NAME, availability.getLatestVersionName());
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
    public ApkUpdateAvailability load() {
        String latestVersionName = store.getString(KEY_LATEST_VERSION_NAME);
        String downloadUrl = store.getString(KEY_DOWNLOAD_URL);
        String assetName = store.getString(KEY_ASSET_NAME);
        if (latestVersionName == null || downloadUrl == null || assetName == null) {
            return null;
        }
        long expectedSizeBytes = parseExpectedSizeBytes(store.getString(KEY_EXPECTED_SIZE_BYTES));
        String downloadedFilePath = store.getString(KEY_DOWNLOADED_FILE_PATH);
        return ApkUpdateAvailability.available(latestVersionName, downloadUrl, assetName, expectedSizeBytes)
            .withDownloadedFilePath(downloadedFilePath);
    }

    @Nullable
    public ApkUpdateAvailability loadIfNewerThanInstalled(String installedVersionName) {
        ApkUpdateAvailability availability = load();
        if (availability == null) {
            return null;
        }
        if (!versionComparator.isNewer(availability.getLatestVersionName(), installedVersionName)) {
            clear();
            return null;
        }
        return availability;
    }

    public boolean hasPending() {
        return load() != null;
    }

    public void clear() {
        store.remove(KEY_LATEST_VERSION_NAME);
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
