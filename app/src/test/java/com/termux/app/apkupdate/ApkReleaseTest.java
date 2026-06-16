package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ApkReleaseTest {

    @Test
    public void exposesVersionNameTagNameAndAssets() {
        List<ReleaseAsset> assets = Arrays.asList(
            new ReleaseAsset("a.apk", "https://example.com/a"),
            new ReleaseAsset("b.apk", "https://example.com/b"));

        ApkRelease release = new ApkRelease("0.119.0", "v0.119.0", assets);

        Assert.assertEquals("0.119.0", release.getVersionName());
        Assert.assertEquals("v0.119.0", release.getTagName());
        Assert.assertEquals(2, release.getAssets().size());
        Assert.assertEquals("a.apk", release.getAssets().get(0).getName());
        Assert.assertEquals("b.apk", release.getAssets().get(1).getName());
    }

    @Test
    public void supportsEmptyAssetList() {
        ApkRelease release = new ApkRelease("1.0.0", "v1.0.0", Collections.emptyList());

        Assert.assertTrue(release.getAssets().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void returnedAssetListIsUnmodifiable() {
        ApkRelease release = new ApkRelease("1.0.0", "v1.0.0", new ArrayList<>());

        release.getAssets().add(new ReleaseAsset("c.apk", "https://example.com/c"));
    }

    @Test
    public void exposesAssetsInProvidedOrder() {
        List<ReleaseAsset> source = new ArrayList<>();
        source.add(new ReleaseAsset("first.apk", "https://example.com/first"));
        source.add(new ReleaseAsset("second.apk", "https://example.com/second"));

        ApkRelease release = new ApkRelease("1.0.0", "v1.0.0", source);

        Assert.assertEquals("first.apk", release.getAssets().get(0).getName());
        Assert.assertEquals("second.apk", release.getAssets().get(1).getName());
    }
}
