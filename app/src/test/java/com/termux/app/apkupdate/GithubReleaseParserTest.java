package com.termux.app.apkupdate;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GithubReleaseParserTest {

    private final GithubReleaseParser parser = new GithubReleaseParser();

    @Test
    public void parsesVersionNameFromTagAndAssets() throws JSONException {
        String json = "{"
            + "\"tag_name\":\"v0.119.0\","
            + "\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/arm64\"},"
            + "{\"name\":\"termux-app_universal.apk\",\"browser_download_url\":\"https://example.com/universal\"}"
            + "]}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertEquals("0.119.0", release.getVersionName());
        Assert.assertEquals("v0.119.0", release.getTagName());
        List<ReleaseAsset> assets = release.getAssets();
        Assert.assertEquals(2, assets.size());
        Assert.assertEquals("termux-app_arm64-v8a.apk", assets.get(0).getName());
        Assert.assertEquals("https://example.com/arm64", assets.get(0).getDownloadUrl());
    }

    @Test
    public void extractsAssetSizeFromReleaseJson() throws JSONException {
        String json = "{"
            + "\"tag_name\":\"v0.119.0\","
            + "\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/arm64\","
            + "\"size\":123456789}"
            + "]}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertEquals(123456789L, release.getAssets().get(0).getSize());
    }

    @Test
    public void defaultsAssetSizeToZeroWhenSizeKeyMissing() throws JSONException {
        String json = "{"
            + "\"tag_name\":\"v0.119.0\","
            + "\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/arm64\"}"
            + "]}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertEquals(0L, release.getAssets().get(0).getSize());
    }

    @Test
    public void stripsBuildMetadataFromTagName() throws JSONException {
        String json = "{\"tag_name\":\"v0.119.0+abcdef1\",\"assets\":[]}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertEquals("0.119.0", release.getVersionName());
    }

    @Test
    public void returnsEmptyAssetListWhenAssetsKeyMissing() throws JSONException {
        String json = "{\"tag_name\":\"v0.119.0\"}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertTrue(release.getAssets().isEmpty());
    }

    @Test(expected = JSONException.class)
    public void throwsWhenTagNameMissing() throws JSONException {
        parser.parseLatestRelease("{\"assets\":[]}");
    }

    @Test
    public void parsesPerBuildPatchSchemeTag() throws JSONException {
        String json = "{\"tag_name\":\"v0.118.119\",\"assets\":[]}";

        ApkRelease release = parser.parseLatestRelease(json);

        Assert.assertEquals("0.118.119", release.getVersionName());
        Assert.assertEquals("v0.118.119", release.getTagName());
    }
}
