package com.termux.app.apkupdate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GithubReleaseListParser {

    private final ReleaseTagVersionNameResolver tagVersionNameResolver = new ReleaseTagVersionNameResolver();

    public List<ApkRelease> parseReleases(String json) throws JSONException {
        JSONArray root = new JSONArray(json);
        List<ApkRelease> releases = new ArrayList<>();
        for (int releaseIndex = 0; releaseIndex < root.length(); releaseIndex++) {
            JSONObject release = root.getJSONObject(releaseIndex);
            if (release.optBoolean("draft", false)) {
                continue;
            }
            String tagName = release.getString("tag_name");
            String versionName = tagVersionNameResolver.resolveFromTag(tagName);
            releases.add(new ApkRelease(versionName, tagName, parseAssets(release)));
        }
        return releases;
    }

    private List<ReleaseAsset> parseAssets(JSONObject release) throws JSONException {
        List<ReleaseAsset> assets = new ArrayList<>();
        JSONArray assetArray = release.optJSONArray("assets");
        if (assetArray != null) {
            for (int assetIndex = 0; assetIndex < assetArray.length(); assetIndex++) {
                JSONObject asset = assetArray.getJSONObject(assetIndex);
                String name = asset.getString("name");
                String downloadUrl = asset.getString("browser_download_url");
                long size = asset.optLong("size", 0L);
                assets.add(new ReleaseAsset(name, downloadUrl, size));
            }
        }
        return assets;
    }
}
