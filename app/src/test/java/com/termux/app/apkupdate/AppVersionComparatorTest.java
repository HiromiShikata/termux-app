package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class AppVersionComparatorTest {

    private final AppVersionComparator comparator = new AppVersionComparator();

    @Test
    public void isNewerWhenPatchComponentIncreases() {
        Assert.assertTrue(comparator.isNewer("0.118.1", "0.118.0"));
    }

    @Test
    public void isNewerWhenMinorComponentIncreases() {
        Assert.assertTrue(comparator.isNewer("0.119.0", "0.118.0"));
    }

    @Test
    public void isNewerWhenMajorComponentIncreases() {
        Assert.assertTrue(comparator.isNewer("1.0.0", "0.118.0"));
    }

    @Test
    public void isNotNewerWhenVersionsAreEqual() {
        Assert.assertFalse(comparator.isNewer("0.118.0", "0.118.0"));
    }

    @Test
    public void isNotNewerWhenCandidateIsOlder() {
        Assert.assertFalse(comparator.isNewer("0.117.9", "0.118.0"));
    }

    @Test
    public void ignoresBuildMetadataWhenComparing() {
        Assert.assertFalse(comparator.isNewer("0.118.0+abcdef1", "0.118.0"));
        Assert.assertTrue(comparator.isNewer("0.119.0+abcdef1", "0.118.0"));
    }

    @Test
    public void treatsMissingComponentsAsZero() {
        Assert.assertTrue(comparator.isNewer("0.118", "0.117.9"));
        Assert.assertFalse(comparator.isNewer("0.118", "0.118.0"));
    }

    @Test
    public void detectsNewerBuildUnderPerBuildPatchScheme() {
        Assert.assertTrue(comparator.isNewer("0.118.119", "0.118.118"));
    }

    @Test
    public void reportsNoUpdateWhenPerBuildPatchSchemeVersionsAreEqual() {
        Assert.assertFalse(comparator.isNewer("0.118.119", "0.118.119"));
    }

    @Test
    public void staticInstalledBuildSeesFirstBumpedReleaseAsNewer() {
        Assert.assertTrue(comparator.isNewer("0.118.1", "0.118.0"));
    }

    @Test
    public void ignoresInstalledBuildMetadataUnderPerBuildPatchScheme() {
        Assert.assertTrue(comparator.isNewer("0.118.130", "0.118.125+abcdef1"));
        Assert.assertFalse(comparator.isNewer("0.118.125", "0.118.125+abcdef1"));
    }

    @Test
    public void treatsLeadingVTagAsEqualToPlainVersionName() {
        Assert.assertEquals(0, comparator.compare("v0.118.2535", "0.118.2535"));
        Assert.assertFalse(comparator.isNewer("v0.118.2535", "0.118.2535"));
        Assert.assertFalse(comparator.isNewer("0.118.2535", "v0.118.2535"));
    }

    @Test
    public void treatsUppercaseLeadingVTagAsEqualToPlainVersionName() {
        Assert.assertEquals(0, comparator.compare("V0.118.2535", "0.118.2535"));
    }

    @Test
    public void detectsNewerTagAgainstInstalledVersionNameIgnoringLeadingV() {
        Assert.assertTrue(comparator.isNewer("v0.118.2536", "0.118.2535"));
        Assert.assertFalse(comparator.isNewer("v0.118.2534", "0.118.2535"));
    }
}
