package com.termux.app.apkupdate;

public final class ApkUpdateGuide {

    public static final String RECOMMENDED_ARTIFACT_NAME_PREFIX = "termux-app-RECOMMENDED-INSTALL-THIS";

    public static final String BUILD_LIST_URL =
        "https://github.com/HiromiShikata/termux-app/actions/workflows/debug_build.yml?query=branch%3Amain";

    public static final String RELEASES_LATEST_API_URL =
        "https://api.github.com/repos/HiromiShikata/termux-app/releases/latest";

    /**
     * Rate-limit-resilient fallback source for the latest release. The Atom feed is served from the
     * github.com web host and is not subject to the 60-requests-per-hour unauthenticated REST limit
     * that {@link #RELEASES_LATEST_API_URL} enforces.
     */
    public static final String RELEASES_ATOM_FEED_URL =
        "https://github.com/HiromiShikata/termux-app/releases.atom";

    public static final String RELEASES_OWNER = "HiromiShikata";

    public static final String RELEASES_REPO = "termux-app";

    public static final String RELEASES_PAGE_URL =
        "https://github.com/HiromiShikata/termux-app/releases/latest";

    public String getRecommendedArtifactNamePrefix() {
        return RECOMMENDED_ARTIFACT_NAME_PREFIX;
    }

    public String getBuildListUrl() {
        return BUILD_LIST_URL;
    }

    public String getReleasesLatestApiUrl() {
        return RELEASES_LATEST_API_URL;
    }

    public String getReleasesAtomFeedUrl() {
        return RELEASES_ATOM_FEED_URL;
    }

    public String getReleasesOwner() {
        return RELEASES_OWNER;
    }

    public String getReleasesRepo() {
        return RELEASES_REPO;
    }

    public String getReleasesPageUrl() {
        return RELEASES_PAGE_URL;
    }
}
