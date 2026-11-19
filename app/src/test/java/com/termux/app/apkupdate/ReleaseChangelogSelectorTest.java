package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReleaseChangelogSelectorTest {

    private static final class SelectionCase {
        final String description;
        final List<String> releaseVersionNamesNewestFirst;
        final String installedVersionName;
        final List<String> expectedVersionNames;

        SelectionCase(String description, List<String> releaseVersionNamesNewestFirst,
                      String installedVersionName, List<String> expectedVersionNames) {
            this.description = description;
            this.releaseVersionNamesNewestFirst = releaseVersionNamesNewestFirst;
            this.installedVersionName = installedVersionName;
            this.expectedVersionNames = expectedVersionNames;
        }
    }

    private static final SelectionCase[] SELECTION_CASES = {
        new SelectionCase(
            "returns only releases strictly newer than installed version",
            Arrays.asList("0.119.4241", "0.119.4240", "0.119.4000", "0.119.3999"),
            "0.119.4000",
            Arrays.asList("0.119.4241", "0.119.4240")),
        new SelectionCase(
            "returns nothing when installed version equals the newest release",
            Arrays.asList("0.119.4241", "0.119.4240", "0.119.4000"),
            "0.119.4241",
            Collections.<String>emptyList()),
        new SelectionCase(
            "includes a leading v tag name compared without its prefix",
            Arrays.asList("v0.120.1", "v0.119.9999", "0.119.9998"),
            "0.119.9999",
            Collections.singletonList("v0.120.1")),
        new SelectionCase(
            "returns nothing for empty release list",
            Collections.<String>emptyList(),
            "0.119.4000",
            Collections.<String>emptyList()),
        new SelectionCase(
            "preserves input order for interleaved newer releases",
            Arrays.asList("0.120.0", "0.118.0", "0.119.5", "0.119.4001", "0.119.4000"),
            "0.119.4000",
            Arrays.asList("0.120.0", "0.119.4001")),
        new SelectionCase(
            "compares build component numerically rather than lexically",
            Arrays.asList("0.119.10000", "0.119.9999"),
            "0.119.9999",
            Collections.singletonList("0.119.10000")),
        new SelectionCase(
            "treats a missing component as zero",
            Arrays.asList("0.119.1", "0.119.0"),
            "0.119",
            Collections.singletonList("0.119.1")),
    };

    @Test
    public void selectsReleasesStrictlyNewerThanInstalledVersionInOrder() {
        for (SelectionCase selectionCase : SELECTION_CASES) {
            List<ApkRelease> releases = new ArrayList<>();
            for (String versionName : selectionCase.releaseVersionNamesNewestFirst) {
                releases.add(new ApkRelease(versionName, ""));
            }

            List<ApkRelease> selected = ReleaseChangelogSelector.selectNewerThan(
                releases, selectionCase.installedVersionName);

            List<String> selectedVersionNames = new ArrayList<>();
            for (ApkRelease release : selected) {
                selectedVersionNames.add(release.getVersionName());
            }
            Assert.assertEquals(selectionCase.description, selectionCase.expectedVersionNames,
                selectedVersionNames);
        }
    }
}
