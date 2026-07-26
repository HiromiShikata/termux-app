package com.termux.app.apkupdate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AtomReleaseJsonSynthesizer {

    private static final String ASSET_NAME_PREFIX = "termux-app_";
    private static final String ASSET_NAME_SUFFIX = ".apk";
    private static final String UNIVERSAL_ASSET_NAME = "termux-app_universal.apk";

    private static final String[] ABI_ASSET_INFIXES = {
        "arm64-v8a",
        "armeabi-v7a",
        "x86_64",
        "x86",
    };

    private final String downloadBaseUrl;

    public AtomReleaseJsonSynthesizer(String owner, String repo) {
        this.downloadBaseUrl = "https://github.com/" + owner + "/" + repo + "/releases/download/";
    }

    public String synthesizeReleaseJson(String tagName) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("tag_name", tagName);

        JSONArray assets = new JSONArray();
        for (String abiInfix : ABI_ASSET_INFIXES) {
            String assetName = ASSET_NAME_PREFIX + abiInfix + ASSET_NAME_SUFFIX;
            assets.put(assetJson(tagName, assetName));
        }
        assets.put(assetJson(tagName, UNIVERSAL_ASSET_NAME));
        root.put("assets", assets);

        return root.toString();
    }

    private JSONObject assetJson(String tagName, String assetName) throws JSONException {
        JSONObject asset = new JSONObject();
        asset.put("name", assetName);
        asset.put("browser_download_url", downloadBaseUrl + tagName + "/" + assetName);
        return asset;
    }
}
