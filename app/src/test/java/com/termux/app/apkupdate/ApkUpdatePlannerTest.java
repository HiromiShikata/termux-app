package com.termux.app.apkupdate;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdatePlannerTest {

    private final ApkUpdatePlanner planner = new ApkUpdatePlanner();

    private String releaseJson(String tagName) {
        return "{"
            + "\"tag_name\":\"" + tagName + "\","
            + "\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/arm64\"},"
            + "{\"name\":\"termux-app_universal.apk\",\"browser_download_url\":\"https://example.com/universal\"}"
            + "]}";
    }

    @Test
    public void reportsUpdateAvailableWithMatchingAbiAssetWhenReleaseIsNewer() throws JSONException {
        ApkUpdateAvailability availability =
            planner.plan(releaseJson("v0.119.0"), "0.118.0", new String[]{"arm64-v8a"});

        Assert.assertTrue(availability.isUpdateAvailable());
        Assert.assertEquals("0.119.0", availability.getLatestVersionName());
        Assert.assertEquals("https://example.com/arm64", availability.getDownloadUrl());
        Assert.assertEquals("termux-app_arm64-v8a.apk", availability.getAssetName());
    }

    @Test
    public void fallsBackToUniversalAssetWhenAbiHasNoDedicatedApk() throws JSONException {
        ApkUpdateAvailability availability =
            planner.plan(releaseJson("v0.119.0"), "0.118.0", new String[]{"riscv64"});

        Assert.assertTrue(availability.isUpdateAvailable());
        Assert.assertEquals("https://example.com/universal", availability.getDownloadUrl());
    }

    @Test
    public void reportsUpToDateWhenReleaseIsNotNewer() throws JSONException {
        ApkUpdateAvailability availability =
            planner.plan(releaseJson("v0.118.0"), "0.118.0", new String[]{"arm64-v8a"});

        Assert.assertFalse(availability.isUpdateAvailable());
        Assert.assertEquals("0.118.0", availability.getLatestVersionName());
        Assert.assertNull(availability.getDownloadUrl());
    }

    @Test
    public void reportsUpToDateWhenNewerReleaseHasNoUsableAsset() throws JSONException {
        String json = "{\"tag_name\":\"v0.119.0\",\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/arm64\"}"
            + "]}";

        ApkUpdateAvailability availability = planner.plan(json, "0.118.0", new String[]{"x86"});

        Assert.assertFalse(availability.isUpdateAvailable());
    }
}
