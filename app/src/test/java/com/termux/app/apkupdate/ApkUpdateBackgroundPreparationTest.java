package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ApkUpdateBackgroundPreparationTest {

    private static final String INSTALLED_VERSION = "0.119.4000";
    private static final String CACHED_PATH = "cached.apk";
    private static final String DOWNLOADED_PATH = "downloaded.apk";
    private static final String NEWER_CHANGES_TEXT = "0.120.0\n- Feature X\n\n0.119.4241\n- Fix A\n- Fix B";

    private static final List<ApkRelease> NEWER_RELEASES_NEWEST_FIRST = Arrays.asList(
        new ApkRelease("0.120.0", "- Feature X"),
        new ApkRelease("0.119.4241", "- Fix A\n- Fix B"),
        new ApkRelease("0.119.4000", "- Old")
    );

    private static final List<ApkRelease> NO_NEWER_RELEASES_NEWEST_FIRST = Collections.singletonList(
        new ApkRelease("0.119.4000", "- Old")
    );

    private static final class Case {
        final String name;
        final String cachedPath;
        final boolean downloadFails;
        final boolean releaseListFails;
        final List<ApkRelease> releasesNewestFirst;
        final ApkUpdateBackgroundPreparation.Outcome expectedOutcome;
        final List<String> expectedCalls;
        final String expectedNotifiedFilePath;
        final String expectedChangesText;

        Case(String name, String cachedPath, boolean downloadFails, boolean releaseListFails,
             List<ApkRelease> releasesNewestFirst, ApkUpdateBackgroundPreparation.Outcome expectedOutcome,
             List<String> expectedCalls, String expectedNotifiedFilePath, String expectedChangesText) {
            this.name = name;
            this.cachedPath = cachedPath;
            this.downloadFails = downloadFails;
            this.releaseListFails = releaseListFails;
            this.releasesNewestFirst = releasesNewestFirst;
            this.expectedOutcome = expectedOutcome;
            this.expectedCalls = expectedCalls;
            this.expectedNotifiedFilePath = expectedNotifiedFilePath;
            this.expectedChangesText = expectedChangesText;
        }
    }

    private static List<Case> cases() {
        return Arrays.asList(
            new Case("cache hit with newer releases notifies with the cached file",
                CACHED_PATH, false, false, NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.NOTIFIED,
                Arrays.asList("cached", "releases", "notify"),
                CACHED_PATH, NEWER_CHANGES_TEXT),
            new Case("cache miss downloads and then notifies with the downloaded file",
                null, false, false, NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.NOTIFIED,
                Arrays.asList("download", "releases", "notify"),
                DOWNLOADED_PATH, NEWER_CHANGES_TEXT),
            new Case("cache miss and download failure stops before the release list",
                null, true, false, NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.DOWNLOAD_FAILED,
                Collections.singletonList("download"),
                null, null),
            new Case("cache miss and release list failure skips notification",
                null, false, true, NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.RELEASE_LIST_FAILED,
                Arrays.asList("download", "releases"),
                null, null),
            new Case("cache hit and release list failure skips notification",
                CACHED_PATH, false, true, NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.RELEASE_LIST_FAILED,
                Arrays.asList("cached", "releases"),
                null, null),
            new Case("cache miss with no release newer than installed skips notification",
                null, false, false, NO_NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.NO_NEWER_RELEASE,
                Arrays.asList("download", "releases"),
                null, null),
            new Case("cache hit with no release newer than installed skips notification",
                CACHED_PATH, false, false, NO_NEWER_RELEASES_NEWEST_FIRST,
                ApkUpdateBackgroundPreparation.Outcome.NO_NEWER_RELEASE,
                Arrays.asList("cached", "releases"),
                null, null)
        );
    }

    private static final class RecordingSteps {
        final List<String> calls = new ArrayList<>();
        String notifiedFilePath;
        String notifiedChangesText;

        private final Case testCase;

        RecordingSteps(Case testCase) {
            this.testCase = testCase;
        }

        ApkUpdateBackgroundPreparation.CachedApkStep cachedApk() {
            return new ApkUpdateBackgroundPreparation.CachedApkStep() {
                @Override
                public File findValidatedCachedApk(ApkUpdateAvailability availability) {
                    calls.add("cached");
                    return testCase.cachedPath == null ? null : new File(testCase.cachedPath);
                }
            };
        }

        ApkUpdateBackgroundPreparation.DownloadStep download() {
            return new ApkUpdateBackgroundPreparation.DownloadStep() {
                @Override
                public File download(ApkUpdateAvailability availability) throws IOException {
                    calls.add("download");
                    if (testCase.downloadFails) {
                        throw new IOException("download failed");
                    }
                    return new File(DOWNLOADED_PATH);
                }
            };
        }

        ApkUpdateBackgroundPreparation.ReleaseListStep releases() {
            return new ApkUpdateBackgroundPreparation.ReleaseListStep() {
                @Override
                public List<ApkRelease> fetchReleasesNewestFirst() throws IOException {
                    calls.add("releases");
                    if (testCase.releaseListFails) {
                        throw new IOException("release list failed");
                    }
                    return testCase.releasesNewestFirst;
                }
            };
        }

        ApkUpdateBackgroundPreparation.NotificationStep notification() {
            return new ApkUpdateBackgroundPreparation.NotificationStep() {
                @Override
                public void notifyWithChanges(ApkUpdateAvailability availability, File apkFile,
                                              String changesText) {
                    calls.add("notify");
                    notifiedFilePath = apkFile.getPath();
                    notifiedChangesText = changesText;
                }
            };
        }
    }

    @Test
    public void runReturnsOutcomeCallSequenceAndChangesTextForEachCase() throws IOException {
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.120.0", "https://example.com/termux-app.apk", "termux-app.apk");

        for (Case testCase : cases()) {
            RecordingSteps steps = new RecordingSteps(testCase);
            ApkUpdateBackgroundPreparation preparation = new ApkUpdateBackgroundPreparation(
                INSTALLED_VERSION, steps.cachedApk(), steps.download(), steps.releases(), steps.notification());

            ApkUpdateBackgroundPreparation.Outcome outcome = preparation.run(availability);

            Assert.assertEquals(testCase.name, testCase.expectedOutcome, outcome);
            Assert.assertEquals(testCase.name, testCase.expectedCalls, steps.calls);
            Assert.assertEquals(testCase.name, testCase.expectedNotifiedFilePath, steps.notifiedFilePath);
            Assert.assertEquals(testCase.name, testCase.expectedChangesText, steps.notifiedChangesText);
        }
    }
}
