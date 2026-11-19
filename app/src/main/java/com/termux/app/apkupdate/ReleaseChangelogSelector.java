package com.termux.app.apkupdate;

import java.util.ArrayList;
import java.util.List;

public final class ReleaseChangelogSelector {

    private ReleaseChangelogSelector() {
    }

    public static List<ApkRelease> selectNewerThan(List<ApkRelease> releasesNewestFirst, String installedVersionName) {
        AppVersionComparator versionComparator = new AppVersionComparator();
        List<ApkRelease> newerReleases = new ArrayList<>();
        for (ApkRelease release : releasesNewestFirst) {
            if (versionComparator.isNewer(release.getVersionName(), installedVersionName)) {
                newerReleases.add(release);
            }
        }
        return newerReleases;
    }
}
