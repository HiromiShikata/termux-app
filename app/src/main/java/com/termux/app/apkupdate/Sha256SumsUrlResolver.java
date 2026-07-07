package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

public final class Sha256SumsUrlResolver {

    private static final String SUMS_FILE_NAME = "termux-app_sha256sums";

    @Nullable
    public String resolveFromAssetDownloadUrl(String assetDownloadUrl) {
        if (assetDownloadUrl == null) {
            return null;
        }
        int lastSlashIndex = assetDownloadUrl.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return null;
        }
        return assetDownloadUrl.substring(0, lastSlashIndex + 1) + SUMS_FILE_NAME;
    }
}
