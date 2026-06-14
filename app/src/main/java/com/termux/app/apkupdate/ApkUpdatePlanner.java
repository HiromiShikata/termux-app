package com.termux.app.apkupdate;

import org.json.JSONException;

public final class ApkUpdatePlanner {

    private final GithubReleaseParser releaseParser;
    private final AppVersionComparator versionComparator;
    private final ApkAbiAssetSelector assetSelector;

    public ApkUpdatePlanner() {
        this(new GithubReleaseParser(), new AppVersionComparator(), new ApkAbiAssetSelector());
    }

    public ApkUpdatePlanner(GithubReleaseParser releaseParser, AppVersionComparator versionComparator,
                            ApkAbiAssetSelector assetSelector) {
        this.releaseParser = releaseParser;
        this.versionComparator = versionComparator;
        this.assetSelector = assetSelector;
    }

    public ApkUpdateAvailability plan(String releasesLatestJson, String currentVersionName,
                                      String[] supportedAbis) throws JSONException {
        ApkRelease release = releaseParser.parseLatestRelease(releasesLatestJson);
        boolean isNewer = versionComparator.isNewer(release.getVersionName(), currentVersionName);
        if (!isNewer) {
            return ApkUpdateAvailability.upToDate(release.getVersionName());
        }
        ReleaseAsset asset = assetSelector.selectForAbis(release.getAssets(), supportedAbis);
        if (asset == null) {
            return ApkUpdateAvailability.upToDate(release.getVersionName());
        }
        return ApkUpdateAvailability.available(release.getVersionName(), asset.getDownloadUrl(), asset.getName());
    }
}
