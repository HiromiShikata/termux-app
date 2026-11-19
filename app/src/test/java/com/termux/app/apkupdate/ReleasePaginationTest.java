package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReleasePaginationTest {

    private static final String INSTALLED_VERSION = "0.119.4000";

    private static final class Case {
        final String name;
        final List<ApkRelease> pageNewestFirst;
        final int pageSize;
        final boolean expected;

        Case(String name, List<ApkRelease> pageNewestFirst, int pageSize, boolean expected) {
            this.name = name;
            this.pageNewestFirst = pageNewestFirst;
            this.pageSize = pageSize;
            this.expected = expected;
        }
    }

    private static List<ApkRelease> releases(String... versionNames) {
        List<ApkRelease> releases = new ArrayList<>();
        for (String versionName : versionNames) {
            releases.add(new ApkRelease(versionName, ""));
        }
        return releases;
    }

    private static List<ApkRelease> newerPageOfSize(int size) {
        List<ApkRelease> releases = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            releases.add(new ApkRelease("0.120." + (size - index), ""));
        }
        return releases;
    }

    private static List<Case> cases() {
        List<ApkRelease> fullPageOf100WithInstalledLast = newerPageOfSize(100);
        fullPageOf100WithInstalledLast.set(99, new ApkRelease(INSTALLED_VERSION, ""));

        return Arrays.asList(
            new Case("empty page has no more pages", Collections.<ApkRelease>emptyList(), 3, false),
            new Case("page shorter than page size has no more pages",
                releases("0.120.0", "0.119.4241"), 3, false),
            new Case("full page of newer releases reads the next page",
                releases("0.120.0", "0.119.4241", "0.119.4100"), 3, true),
            new Case("full page with an installed-version release in the middle stops",
                releases("0.120.0", "0.119.4000", "0.119.3999"), 3, false),
            new Case("full page whose last release equals installed stops",
                releases("0.120.0", "0.119.4241", "0.119.4000"), 3, false),
            new Case("full page of releases all older than installed stops",
                releases("0.119.3999", "0.119.3998", "0.119.3997"), 3, false),
            new Case("full page of 100 newer releases reads the next page",
                newerPageOfSize(100), 100, true),
            new Case("full page of 100 whose last release equals installed stops",
                fullPageOf100WithInstalledLast, 100, false)
        );
    }

    @Test
    public void hasMorePagesToReadReturnsTrueOnlyForFullPagesOfNewerReleases() {
        for (Case testCase : cases()) {
            Assert.assertEquals(testCase.name, testCase.expected,
                ReleasePagination.hasMorePagesToRead(testCase.pageNewestFirst, testCase.pageSize, INSTALLED_VERSION));
        }
    }
}
