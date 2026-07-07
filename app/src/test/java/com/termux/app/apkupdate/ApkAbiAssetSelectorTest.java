package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApkAbiAssetSelectorTest {

    private final ApkAbiAssetSelector selector = new ApkAbiAssetSelector();

    private List<ReleaseAsset> sampleAssets() {
        return new ArrayList<>(Arrays.asList(
            new ReleaseAsset("termux-app_universal.apk", "https://example.com/universal", 0L),
            new ReleaseAsset("termux-app_arm64-v8a.apk", "https://example.com/arm64", 0L),
            new ReleaseAsset("termux-app_armeabi-v7a.apk", "https://example.com/armeabi", 0L),
            new ReleaseAsset("termux-app_x86_64.apk", "https://example.com/x86_64", 0L),
            new ReleaseAsset("termux-app_x86.apk", "https://example.com/x86", 0L),
            new ReleaseAsset("termux-app_sha256sums", "https://example.com/sums", 0L)));
    }

    @Test
    public void selectsTheFirstSupportedAbiAsset() {
        ReleaseAsset selected = selector.selectForAbis(sampleAssets(),
            new String[]{"arm64-v8a", "armeabi-v7a"});
        Assert.assertNotNull(selected);
        Assert.assertEquals("termux-app_arm64-v8a.apk", selected.getName());
    }

    @Test
    public void doesNotConfuseX86WithX8664() {
        ReleaseAsset selected = selector.selectForAbis(sampleAssets(), new String[]{"x86"});
        Assert.assertNotNull(selected);
        Assert.assertEquals("termux-app_x86.apk", selected.getName());
    }

    @Test
    public void fallsBackToUniversalWhenNoAbiMatches() {
        ReleaseAsset selected = selector.selectForAbis(sampleAssets(), new String[]{"riscv64"});
        Assert.assertNotNull(selected);
        Assert.assertEquals("termux-app_universal.apk", selected.getName());
    }

    @Test
    public void skipsNullAndEmptyAbiEntries() {
        ReleaseAsset selected = selector.selectForAbis(sampleAssets(),
            new String[]{null, "", "arm64-v8a"});
        Assert.assertNotNull(selected);
        Assert.assertEquals("termux-app_arm64-v8a.apk", selected.getName());
    }

    @Test
    public void returnsNullWhenNoAbiMatchesAndNoUniversalAssetExists() {
        List<ReleaseAsset> assets = new ArrayList<>(Arrays.asList(
            new ReleaseAsset("termux-app_arm64-v8a.apk", "https://example.com/arm64", 0L)));
        ReleaseAsset selected = selector.selectForAbis(assets, new String[]{"x86"});
        Assert.assertNull(selected);
    }
}
