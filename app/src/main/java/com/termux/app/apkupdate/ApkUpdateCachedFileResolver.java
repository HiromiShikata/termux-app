package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.io.File;

public final class ApkUpdateCachedFileResolver {

    private final ApkFileValidator apkFileValidator;

    public ApkUpdateCachedFileResolver() {
        this(new ApkFileValidator());
    }

    public ApkUpdateCachedFileResolver(ApkFileValidator apkFileValidator) {
        this.apkFileValidator = apkFileValidator;
    }

    @Nullable
    public File resolveExistingFile(ApkUpdateAvailability availability) {
        if (availability == null || !availability.hasDownloadedFilePath()) {
            return null;
        }
        File file = new File(availability.getDownloadedFilePath());
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        String invalidReason = apkFileValidator.validate(file, null, 0L);
        if (invalidReason != null) {
            file.delete();
            return null;
        }
        return file;
    }
}
