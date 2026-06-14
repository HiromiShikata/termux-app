package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ApkUpdateGuideTest {

    private final ApkUpdateGuide guide = new ApkUpdateGuide();

    @Test
    public void recommendedArtifactNameIsTheClearStableCanonicalName() {
        Assert.assertEquals("termux-app-RECOMMENDED-INSTALL-THIS-arm64-v8a", guide.getRecommendedArtifactName());
    }

    @Test
    public void recommendedArtifactNameDoesNotContainVolatileVersionOrCommitHash() {
        String name = guide.getRecommendedArtifactName();
        Assert.assertFalse(name.contains("+"));
        Assert.assertFalse(name.matches(".*\\bv?\\d+\\.\\d+\\.\\d+.*"));
    }

    @Test
    public void buildListUrlPointsAtTheBuildWorkflowRuns() {
        Assert.assertEquals(
            "https://github.com/HiromiShikata/termux-app/actions/workflows/debug_build.yml?query=branch%3Amain",
            guide.getBuildListUrl());
    }

    @Test
    public void instructionMessageNamesTheSingleArtifactToDownload() {
        String message = guide.buildInstructionMessage();
        Assert.assertTrue(message.contains(guide.getRecommendedArtifactName()));
        Assert.assertTrue(message.toLowerCase().contains("download"));
        Assert.assertTrue(message.toLowerCase().contains("arm64-v8a"));
    }
}
