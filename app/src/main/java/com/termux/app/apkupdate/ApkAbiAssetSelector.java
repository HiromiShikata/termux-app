package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.util.List;

public final class ApkAbiAssetSelector {

    private static final String UNIVERSAL_ASSET_SUFFIX = "_universal.apk";

    @Nullable
    public ReleaseAsset selectForAbis(List<ReleaseAsset> assets, String[] supportedAbis) {
        if (supportedAbis != null) {
            for (String abi : supportedAbis) {
                if (abi == null || abi.isEmpty()) {
                    continue;
                }
                String abiSuffix = "_" + abi + ".apk";
                ReleaseAsset match = findBySuffix(assets, abiSuffix);
                if (match != null) {
                    return match;
                }
            }
        }
        return findBySuffix(assets, UNIVERSAL_ASSET_SUFFIX);
    }

    @Nullable
    private ReleaseAsset findBySuffix(List<ReleaseAsset> assets, String suffix) {
        for (ReleaseAsset asset : assets) {
            if (asset.getName().endsWith(suffix)) {
                return asset;
            }
        }
        return null;
    }
}
