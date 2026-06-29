package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.io.File;

public final class ApkUpdateCachedFileResolver {

    @Nullable
    public File resolveExistingFile(ApkUpdateAvailability availability) {
        if (availability == null || !availability.hasDownloadedFilePath()) {
            return null;
        }
        File file = new File(availability.getDownloadedFilePath());
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return file;
    }
}
