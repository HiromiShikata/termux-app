package com.termux.app.apkupdate;

import java.util.List;

public final class ReleasePagination {

    private ReleasePagination() {
    }

    public static boolean hasMorePagesToRead(List<ApkRelease> pageNewestFirst, int pageSize, String installedVersionName) {
        if (pageNewestFirst.isEmpty() || pageNewestFirst.size() < pageSize) {
            return false;
        }
        AppVersionComparator versionComparator = new AppVersionComparator();
        for (ApkRelease release : pageNewestFirst) {
            if (!versionComparator.isNewer(release.getVersionName(), installedVersionName)) {
                return false;
            }
        }
        return true;
    }
}
