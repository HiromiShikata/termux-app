package com.termux.app.apkupdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PreviousReleaseSelector {

    private final AppVersionComparator versionComparator = new AppVersionComparator();

    public List<ApkRelease> selectOlderThan(List<ApkRelease> releases, String currentVersionName) {
        List<ApkRelease> older = new ArrayList<>();
        for (ApkRelease release : releases) {
            if (versionComparator.compare(release.getVersionName(), currentVersionName) < 0) {
                older.add(release);
            }
        }
        Collections.sort(older, new Comparator<ApkRelease>() {
            @Override
            public int compare(ApkRelease left, ApkRelease right) {
                return versionComparator.compare(right.getVersionName(), left.getVersionName());
            }
        });
        return older;
    }
}
