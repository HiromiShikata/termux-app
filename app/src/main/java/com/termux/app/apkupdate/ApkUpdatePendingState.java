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

    private final Store store;

    public ApkUpdatePendingState(Store store) {
        this.store = store;
    }

    public void save(ApkUpdateAvailability availability) {
        if (!availability.isUpdateAvailable()) {
            clear();
            return;
        }
        store.putString(KEY_LATEST_VERSION_NAME, availability.getLatestVersionName());
        store.putString(KEY_DOWNLOAD_URL, availability.getDownloadUrl());
        store.putString(KEY_ASSET_NAME, availability.getAssetName());
    }

    @Nullable
    public ApkUpdateAvailability load() {
        String latestVersionName = store.getString(KEY_LATEST_VERSION_NAME);
        String downloadUrl = store.getString(KEY_DOWNLOAD_URL);
        String assetName = store.getString(KEY_ASSET_NAME);
        if (latestVersionName == null || downloadUrl == null || assetName == null) {
            return null;
        }
        return ApkUpdateAvailability.available(latestVersionName, downloadUrl, assetName);
    }

    public boolean hasPending() {
        return load() != null;
    }

    public void clear() {
        store.remove(KEY_LATEST_VERSION_NAME);
        store.remove(KEY_DOWNLOAD_URL);
        store.remove(KEY_ASSET_NAME);
    }
}
