package com.termux.app.apkupdate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GithubReleaseParser {

    public ApkRelease parseLatestRelease(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        String tagName = root.getString("tag_name");
        String versionName = versionNameFromTag(tagName);

        List<ReleaseAsset> assets = new ArrayList<>();
        JSONArray assetArray = root.optJSONArray("assets");
        if (assetArray != null) {
            for (int index = 0; index < assetArray.length(); index++) {
                JSONObject asset = assetArray.getJSONObject(index);
                String name = asset.getString("name");
                String downloadUrl = asset.getString("browser_download_url");
                long size = asset.optLong("size", 0L);
                assets.add(new ReleaseAsset(name, downloadUrl, size));
            }
        }
        return new ApkRelease(versionName, tagName, assets);
    }

    private String versionNameFromTag(String tagName) {
        String version = tagName;
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        int buildMetadataIndex = version.indexOf('+');
        if (buildMetadataIndex >= 0) {
            version = version.substring(0, buildMetadataIndex);
        }
        return version;
    }
}
