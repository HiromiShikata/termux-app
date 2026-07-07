package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class Sha256SumsUrlResolverTest {

    private final Sha256SumsUrlResolver resolver = new Sha256SumsUrlResolver();

    @Test
    public void replacesAssetFileNameWithSumsFileName() {
        String sumsUrl = resolver.resolveFromAssetDownloadUrl(
            "https://github.com/HiromiShikata/termux-app/releases/download/v0.119.2821/termux-app_arm64-v8a.apk");

        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/releases/download/v0.119.2821/termux-app_sha256sums",
            sumsUrl);
    }

    @Test
    public void returnsNullForNullUrl() {
        Assert.assertNull(resolver.resolveFromAssetDownloadUrl(null));
    }

    @Test
    public void returnsNullForUrlWithoutSlash() {
        Assert.assertNull(resolver.resolveFromAssetDownloadUrl("termux-app_arm64-v8a.apk"));
    }
}
