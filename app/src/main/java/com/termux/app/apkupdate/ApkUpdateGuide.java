package com.termux.app.apkupdate;

public final class ApkUpdateGuide {

    public static final String RECOMMENDED_ARTIFACT_NAME = "termux-app-RECOMMENDED-INSTALL-THIS-arm64-v8a";

    public static final String BUILD_LIST_URL =
        "https://github.com/HiromiShikata/termux-app/actions/workflows/debug_build.yml?query=branch%3Amain";

    public String getRecommendedArtifactName() {
        return RECOMMENDED_ARTIFACT_NAME;
    }

    public String getBuildListUrl() {
        return BUILD_LIST_URL;
    }

    public String buildInstructionMessage() {
        return "To update, open the latest successful build, scroll to the \"Artifacts\" section, and download:\n\n"
            + RECOMMENDED_ARTIFACT_NAME
            + "\n\nThis is the recommended APK to install on most phones (arm64-v8a). "
            + "The other artifacts are for different CPU architectures or are checksums, "
            + "and you do not need them unless your device requires a specific architecture.";
    }
}
