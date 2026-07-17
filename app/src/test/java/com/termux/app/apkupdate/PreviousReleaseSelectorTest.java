package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreviousReleaseSelectorTest {

    private final PreviousReleaseSelector selector = new PreviousReleaseSelector();

    private ApkRelease release(String versionName) {
        return new ApkRelease(versionName, "v" + versionName, Collections.emptyList());
    }

    @Test
    public void keepsOnlyReleasesOlderThanCurrent() {
        List<ApkRelease> releases = new ArrayList<>();
        releases.add(release("0.119.0"));
        releases.add(release("0.118.0"));
        releases.add(release("0.117.0"));

        List<ApkRelease> older = selector.selectOlderThan(releases, "0.119.0");

        Assert.assertEquals(2, older.size());
        Assert.assertEquals("0.118.0", older.get(0).getVersionName());
        Assert.assertEquals("0.117.0", older.get(1).getVersionName());
    }

    @Test
    public void sortsDescendingRegardlessOfInputOrder() {
        List<ApkRelease> releases = new ArrayList<>();
        releases.add(release("0.115.0"));
        releases.add(release("0.118.0"));
        releases.add(release("0.116.0"));

        List<ApkRelease> older = selector.selectOlderThan(releases, "0.119.0");

        Assert.assertEquals("0.118.0", older.get(0).getVersionName());
        Assert.assertEquals("0.116.0", older.get(1).getVersionName());
        Assert.assertEquals("0.115.0", older.get(2).getVersionName());
    }

    @Test
    public void excludesCurrentAndNewerReleases() {
        List<ApkRelease> releases = new ArrayList<>();
        releases.add(release("0.120.0"));
        releases.add(release("0.119.0"));
        releases.add(release("0.118.0"));

        List<ApkRelease> older = selector.selectOlderThan(releases, "0.119.0");

        Assert.assertEquals(1, older.size());
        Assert.assertEquals("0.118.0", older.get(0).getVersionName());
    }

    @Test
    public void returnsEmptyWhenNothingIsOlder() {
        List<ApkRelease> releases = new ArrayList<>();
        releases.add(release("0.119.0"));

        Assert.assertTrue(selector.selectOlderThan(releases, "0.119.0").isEmpty());
    }
}
