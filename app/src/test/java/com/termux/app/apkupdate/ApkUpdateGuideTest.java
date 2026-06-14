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
    public void instructionMessageGuidesByPrefixToTheSingleArtifactToDownload() {
        String message = guide.buildInstructionMessage();
        Assert.assertTrue(message.contains(guide.getRecommendedArtifactNamePrefix()));
        Assert.assertTrue(message.toLowerCase().contains("download"));
        Assert.assertTrue(message.toLowerCase().contains("arm64-v8a"));
        Assert.assertTrue(message.toLowerCase().contains("starts with"));
    }
}
