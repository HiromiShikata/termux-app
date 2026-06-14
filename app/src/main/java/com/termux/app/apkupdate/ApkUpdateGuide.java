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

    public String buildInstructionMessage() {
        return "To update, open the latest successful build, scroll to the \"Artifacts\" section, and download the one whose name starts with:\n\n"
            + RECOMMENDED_ARTIFACT_NAME_PREFIX
            + "\n\nThis is the recommended APK to install on most phones (arm64-v8a). "
            + "Its full name also includes the build version, so it is different on every build, but the start of the name is always the same. "
            + "The other artifacts are for different CPU architectures or are checksums, "
            + "and you do not need them unless your device requires a specific architecture.";
    }
}
