package com.termux.app.apkupdate;

import androidx.annotation.NonNull;

import java.io.File;

public final class ApkUpdateCachePruner {

    private static final String PARTIAL_FILE_SUFFIX = ".part";

    public void pruneExcept(@NonNull File cacheDirectory, @NonNull String fileNameToKeep) {
        if (!cacheDirectory.isDirectory()) {
            return;
        }
        File[] cachedFiles = cacheDirectory.listFiles();
        if (cachedFiles == null) {
            return;
        }
        String partialFileNameToKeep = fileNameToKeep + PARTIAL_FILE_SUFFIX;
        for (File cachedFile : cachedFiles) {
            if (!cachedFile.isFile()) {
                continue;
            }
            String cachedFileName = cachedFile.getName();
            if (cachedFileName.equals(fileNameToKeep) || cachedFileName.equals(partialFileNameToKeep)) {
                continue;
            }
            cachedFile.delete();
        }
    }
}
