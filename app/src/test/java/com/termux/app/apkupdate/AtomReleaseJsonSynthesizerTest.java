package com.termux.app.apkupdate;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

public class AtomReleaseJsonSynthesizerTest {

    private final AtomReleaseJsonSynthesizer synthesizer =
        new AtomReleaseJsonSynthesizer("HiromiShikata", "termux-app");

    @Test
    public void synthesizedJsonIsParsedIntoReleaseWithExpectedVersion() throws JSONException {
        String json = synthesizer.synthesizeReleaseJson("v0.119.2744");

        ApkRelease release = new GithubReleaseParser().parseLatestRelease(json);

        Assert.assertEquals("0.119.2744", release.getVersionName());
        Assert.assertEquals("v0.119.2744", release.getTagName());
    }

    @Test
    public void synthesizedJsonYieldsAbiSpecificDownloadUrl() throws JSONException {
        String json = synthesizer.synthesizeReleaseJson("v0.119.2744");
        ApkRelease release = new GithubReleaseParser().parseLatestRelease(json);

        ReleaseAsset asset = new ApkAbiAssetSelector()
            .selectForAbis(release.getAssets(), new String[]{"arm64-v8a"});

        Assert.assertNotNull(asset);
        Assert.assertEquals("termux-app_arm64-v8a.apk", asset.getName());
        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/releases/download/v0.119.2744/termux-app_arm64-v8a.apk",
            asset.getDownloadUrl());
    }

    @Test
    public void synthesizedJsonProvidesUniversalFallbackAsset() throws JSONException {
        String json = synthesizer.synthesizeReleaseJson("v0.119.2744");
        ApkRelease release = new GithubReleaseParser().parseLatestRelease(json);

        ReleaseAsset asset = new ApkAbiAssetSelector()
            .selectForAbis(release.getAssets(), new String[]{"mips-unknown"});

        Assert.assertNotNull(asset);
        Assert.assertEquals("termux-app_universal.apk", asset.getName());
        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/releases/download/v0.119.2744/termux-app_universal.apk",
            asset.getDownloadUrl());
    }
}
