package com.termux.app.apkupdate;

import java.util.Collections;
import java.util.List;

public final class ApkRelease {

    private final String versionName;
    private final String tagName;
    private final List<ReleaseAsset> assets;

    public ApkRelease(String versionName, String tagName, List<ReleaseAsset> assets) {
        this.versionName = versionName;
        this.tagName = tagName;
        this.assets = Collections.unmodifiableList(assets);
    }

    public String getVersionName() {
        return versionName;
    }

    public String getTagName() {
        return tagName;
    }

    public List<ReleaseAsset> getAssets() {
        return assets;
    }
}
