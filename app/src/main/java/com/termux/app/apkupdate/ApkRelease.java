package com.termux.app.apkupdate;

import java.util.Collections;
import java.util.List;

public final class ApkRelease {

    private final String versionName;
    private final String tagName;
    private final List<ReleaseAsset> assets;
    private final String body;

    public ApkRelease(String versionName, String body) {
        this(versionName, "", Collections.emptyList(), body);
    }

    public ApkRelease(String versionName, String tagName, List<ReleaseAsset> assets) {
        this(versionName, tagName, assets, "");
    }

    public ApkRelease(String versionName, String tagName, List<ReleaseAsset> assets, String body) {
        this.versionName = versionName;
        this.tagName = tagName;
        this.assets = Collections.unmodifiableList(assets);
        this.body = body != null ? body : "";
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

    public String getBody() {
        return body;
    }
}
