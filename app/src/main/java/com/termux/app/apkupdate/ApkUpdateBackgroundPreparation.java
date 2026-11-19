package com.termux.app.apkupdate;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ApkUpdateBackgroundPreparation {

    public interface CachedApkStep {
        File findValidatedCachedApk(ApkUpdateAvailability availability);
    }

    public interface DownloadStep {
        File download(ApkUpdateAvailability availability) throws IOException;
    }

    public interface ReleaseListStep {
        List<ApkRelease> fetchReleasesNewestFirst() throws IOException;
    }

    public interface NotificationStep {
        void notifyWithChanges(ApkUpdateAvailability availability, File apkFile, String changesText);
    }

    public enum Outcome {
        NOTIFIED,
        NO_NEWER_RELEASE,
        DOWNLOAD_FAILED,
        RELEASE_LIST_FAILED;

        public boolean requestsRetry() {
            return this == RELEASE_LIST_FAILED;
        }
    }

    private final String installedVersionName;
    private final CachedApkStep cachedApkStep;
    private final DownloadStep downloadStep;
    private final ReleaseListStep releaseListStep;
    private final NotificationStep notificationStep;

    public ApkUpdateBackgroundPreparation(String installedVersionName, CachedApkStep cachedApkStep,
                                          DownloadStep downloadStep, ReleaseListStep releaseListStep,
                                          NotificationStep notificationStep) {
        this.installedVersionName = installedVersionName;
        this.cachedApkStep = cachedApkStep;
        this.downloadStep = downloadStep;
        this.releaseListStep = releaseListStep;
        this.notificationStep = notificationStep;
    }

    public Outcome run(ApkUpdateAvailability availability) {
        File apkFile = cachedApkStep.findValidatedCachedApk(availability);
        if (apkFile == null) {
            try {
                apkFile = downloadStep.download(availability);
            } catch (IOException exception) {
                return Outcome.DOWNLOAD_FAILED;
            }
        }
        List<ApkRelease> releasesNewestFirst;
        try {
            releasesNewestFirst = releaseListStep.fetchReleasesNewestFirst();
        } catch (IOException exception) {
            return Outcome.RELEASE_LIST_FAILED;
        }
        List<ApkRelease> newerReleases = ReleaseChangelogSelector.selectNewerThan(releasesNewestFirst, installedVersionName);
        if (newerReleases.isEmpty()) {
            return Outcome.NO_NEWER_RELEASE;
        }
        notificationStep.notifyWithChanges(availability, apkFile, UpdateNotificationTextComposer.composeBigText(newerReleases));
        return Outcome.NOTIFIED;
    }
}
