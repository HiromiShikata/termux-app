package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.io.File;

public final class ApkUpdateCachedFileResolver {

    private static final String PARTIAL_FILE_SUFFIX = ".part";

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
        if (file.getName().endsWith(PARTIAL_FILE_SUFFIX)) {
            return null;
        }
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        String invalidReason = apkFileValidator.validate(file, null, availability.getExpectedSizeBytes());
        if (invalidReason != null) {
            file.delete();
            return null;
        }
        return file;
    }
}
