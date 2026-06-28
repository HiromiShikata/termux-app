package com.termux.app.apkupdate;

import androidx.annotation.NonNull;

import java.io.File;

public final class ApkUpdateCachePruner {

    public void pruneExcept(@NonNull File cacheDirectory, @NonNull String fileNameToKeep) {
        if (!cacheDirectory.isDirectory()) {
            return;
        }
        File[] cachedFiles = cacheDirectory.listFiles();
        if (cachedFiles == null) {
            return;
        }
        for (File cachedFile : cachedFiles) {
            if (!cachedFile.isFile()) {
                continue;
            }
            if (cachedFile.getName().equals(fileNameToKeep)) {
                continue;
            }
            cachedFile.delete();
        }
    }
}
