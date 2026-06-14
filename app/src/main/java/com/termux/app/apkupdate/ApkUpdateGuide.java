package com.termux.app.apkupdate;

public final class ApkUpdateGuide {

    public static final String RECOMMENDED_ARTIFACT_NAME_PREFIX = "termux-app-RECOMMENDED-INSTALL-THIS";

    public static final String BUILD_LIST_URL =
        "https://github.com/HiromiShikata/termux-app/actions/workflows/debug_build.yml?query=branch%3Amain";

    public String getRecommendedArtifactNamePrefix() {
        return RECOMMENDED_ARTIFACT_NAME_PREFIX;
    }

    public String getBuildListUrl() {
        return BUILD_LIST_URL;
    }
}
