package com.termux.app.apkupdate;

public final class ReleaseAsset {

    private final String name;
    private final String downloadUrl;
    private final long size;

    public ReleaseAsset(String name, String downloadUrl, long size) {
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getSize() {
        return size;
    }
}
