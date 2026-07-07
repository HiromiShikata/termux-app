package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseAssetTest {

    @Test
    public void exposesNameDownloadUrlAndSize() {
        ReleaseAsset asset = new ReleaseAsset("termux-app_arm64-v8a.apk", "https://example.com/arm64", 12345L);

        Assert.assertEquals("termux-app_arm64-v8a.apk", asset.getName());
        Assert.assertEquals("https://example.com/arm64", asset.getDownloadUrl());
        Assert.assertEquals(12345L, asset.getSize());
    }

    @Test
    public void retainsEmptyValues() {
        ReleaseAsset asset = new ReleaseAsset("", "", 0L);

        Assert.assertEquals("", asset.getName());
        Assert.assertEquals("", asset.getDownloadUrl());
    }

    @Test
    public void retainsNullValues() {
        ReleaseAsset asset = new ReleaseAsset(null, null, 0L);

        Assert.assertNull(asset.getName());
        Assert.assertNull(asset.getDownloadUrl());
    }
}
