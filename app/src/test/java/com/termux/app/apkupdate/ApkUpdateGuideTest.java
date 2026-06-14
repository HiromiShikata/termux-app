package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ApkUpdateGuideTest {

    private final ApkUpdateGuide guide = new ApkUpdateGuide();

    @Test
    public void recommendedArtifactNamePrefixIsTheStableRecognizableMarker() {
        Assert.assertEquals("termux-app-RECOMMENDED-INSTALL-THIS", guide.getRecommendedArtifactNamePrefix());
    }

    @Test
    public void recommendedArtifactNamePrefixDoesNotContainVolatileVersionOrCommitHash() {
        String prefix = guide.getRecommendedArtifactNamePrefix();
        Assert.assertFalse(prefix.contains("+"));
        Assert.assertFalse(prefix.matches(".*\\bv?\\d+\\.\\d+\\.\\d+.*"));
    }

    @Test
    public void buildListUrlPointsAtTheBuildWorkflowRuns() {
        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/actions/workflows/debug_build.yml?query=branch%3Amain",
            guide.getBuildListUrl());
    }

    @Test
    public void releasesLatestApiUrlPointsAtTheAnonymousReleasesEndpoint() {
        Assert.assertEquals(
            "https://api.github.com/repos/HiromiShikata/termux-app/releases/latest",
            guide.getReleasesLatestApiUrl());
    }

    @Test
    public void releasesPageUrlPointsAtTheHumanReadableLatestRelease() {
        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/releases/latest",
            guide.getReleasesPageUrl());
    }
}
