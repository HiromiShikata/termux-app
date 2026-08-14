package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class AutomaticUpdatePromptPlannerTest {

    private static ApkUpdateAvailability updateWaitingForItsDownload(String latestVersionName) {
        return ApkUpdateAvailability.available(latestVersionName, "https://example.com/app.apk", "app.apk", 0L);
    }

    private static ApkUpdateAvailability downloadedUpdate(String latestVersionName) {
        return updateWaitingForItsDownload(latestVersionName).withDownloadedFilePath("cached/app.apk");
    }

    @Test
    public void anUpdateWhoseApkIsReadyStartsTheSystemInstaller() {
        Assert.assertEquals("a ready APK has to reach the system installer without anything asked first",
            AutomaticUpdatePromptPlanner.Outcome.START_THE_SYSTEM_INSTALLER,
            AutomaticUpdatePromptPlanner.plan(downloadedUpdate("1.2.3")));
    }

    @Test
    public void anUpdateWhoseApkIsNotDownloadedYetOnlyShowsTheFloatingButton() {
        Assert.assertEquals("without a downloaded APK there is nothing to install yet",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_THE_FLOATING_BUTTON_ONLY,
            AutomaticUpdatePromptPlanner.plan(updateWaitingForItsDownload("1.2.3")));
    }

    @Test
    public void anInstalledVersionThatIsAlreadyTheLatestShowsNothing() {
        Assert.assertEquals("being up to date has to leave the screen untouched",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING,
            AutomaticUpdatePromptPlanner.plan(ApkUpdateAvailability.upToDate("1.2.3")));
    }

    @Test
    public void anAbsentAvailabilityShowsNothing() {
        Assert.assertEquals("no availability has to leave the screen untouched",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING,
            AutomaticUpdatePromptPlanner.plan(null));
    }
}
