package com.termux.app.apkupdate;

public final class ReleaseAsset {

    private final String name;
    private final String downloadUrl;

    public ReleaseAsset(String name, String downloadUrl) {
        this.name = name;
        this.downloadUrl = downloadUrl;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
