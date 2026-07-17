package com.termux.app.apkupdate;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GithubReleaseListParserTest {

    private final GithubReleaseListParser parser = new GithubReleaseListParser();

    @Test
    public void parsesReleasesWithAssets() throws JSONException {
        String json = "["
            + "{\"tag_name\":\"v0.119.0+debug\",\"draft\":false,\"assets\":["
            + "{\"name\":\"termux-app_universal.apk\",\"browser_download_url\":\"https://example.com/119_universal.apk\",\"size\":5000000}"
            + "]},"
            + "{\"tag_name\":\"v0.118.0+debug\",\"draft\":false,\"assets\":["
            + "{\"name\":\"termux-app_arm64-v8a.apk\",\"browser_download_url\":\"https://example.com/118_arm64.apk\",\"size\":4000000}"
            + "]}"
            + "]";

        List<ApkRelease> releases = parser.parseReleases(json);

        Assert.assertEquals(2, releases.size());
        Assert.assertEquals("0.119.0", releases.get(0).getVersionName());
        Assert.assertEquals("v0.119.0+debug", releases.get(0).getTagName());
        Assert.assertEquals(1, releases.get(0).getAssets().size());
        Assert.assertEquals("termux-app_universal.apk", releases.get(0).getAssets().get(0).getName());
        Assert.assertEquals("https://example.com/119_universal.apk",
            releases.get(0).getAssets().get(0).getDownloadUrl());
        Assert.assertEquals(5000000L, releases.get(0).getAssets().get(0).getSize());
    }

    @Test
    public void skipsDraftReleases() throws JSONException {
        String json = "["
            + "{\"tag_name\":\"v0.120.0\",\"draft\":true,\"assets\":[]},"
            + "{\"tag_name\":\"v0.119.0\",\"draft\":false,\"assets\":[]}"
            + "]";

        List<ApkRelease> releases = parser.parseReleases(json);

        Assert.assertEquals(1, releases.size());
        Assert.assertEquals("0.119.0", releases.get(0).getVersionName());
    }

    @Test
    public void parsesReleaseWithoutAssetsArray() throws JSONException {
        String json = "[{\"tag_name\":\"v0.119.0\"}]";

        List<ApkRelease> releases = parser.parseReleases(json);

        Assert.assertEquals(1, releases.size());
        Assert.assertTrue(releases.get(0).getAssets().isEmpty());
    }
}
