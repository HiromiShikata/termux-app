package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AutomaticUpdatePromptControllerTest {

    private static final class RecordingIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        final List<String> shownVersions = new ArrayList<>();
        int hideCount;

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
        }

        @Override
        public void hide() {
            hideCount++;
        }
    }

    private static final class RecordingUpdateTrigger
        implements ApkUpdateFloatingIndicatorController.UpdateTrigger {

        final List<ApkUpdateAvailability> startedUpdates = new ArrayList<>();

        @Override
        public void startUpdate(ApkUpdateAvailability availability) {
            startedUpdates.add(availability);
        }
    }

    private final RecordingIndicatorView indicatorView = new RecordingIndicatorView();

    private final RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();

    private final AutomaticUpdatePromptController controller = new AutomaticUpdatePromptController(
        new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger), updateTrigger);

    private static ApkUpdateAvailability updateWaitingForItsDownload(String latestVersionName) {
        return ApkUpdateAvailability.available(latestVersionName, "https://example.com/app.apk", "app.apk", 0L);
    }

    private static ApkUpdateAvailability downloadedUpdate(String latestVersionName) {
        return updateWaitingForItsDownload(latestVersionName).withDownloadedFilePath("cached/app.apk");
    }

    @Test
    public void aDownloadedUpdateReachesTheSystemInstallerWithoutAnyTap() {
        ApkUpdateAvailability availability = downloadedUpdate("1.2.3");

        controller.onUpdateAvailable(availability);

        Assert.assertEquals("the ready APK has to be handed to the system installer by itself",
            1, updateTrigger.startedUpdates.size());
        Assert.assertSame(availability, updateTrigger.startedUpdates.get(0));
    }

    @Test
    public void aDownloadedUpdateAlsoLeavesTheFloatingButtonBehindTheInstaller() {
        controller.onUpdateAvailable(downloadedUpdate("1.2.3"));

        Assert.assertEquals("the button has to stay available for a cancelled installer",
            1, indicatorView.shownVersions.size());
        Assert.assertEquals("1.2.3", indicatorView.shownVersions.get(0));
    }

    @Test
    public void anUpdateWhoseApkIsNotDownloadedYetOnlySurfacesTheFloatingButton() {
        controller.onUpdateAvailable(updateWaitingForItsDownload("1.2.3"));

        Assert.assertTrue("nothing may be installed while no APK is on the device",
            updateTrigger.startedUpdates.isEmpty());
        Assert.assertEquals(1, indicatorView.shownVersions.size());
    }

    @Test
    public void anUpToDateAvailabilityStartsNothingAndHidesTheButton() {
        controller.onUpdateAvailable(ApkUpdateAvailability.upToDate("1.2.3"));

        Assert.assertTrue(updateTrigger.startedUpdates.isEmpty());
        Assert.assertTrue(indicatorView.shownVersions.isEmpty());
        Assert.assertEquals(1, indicatorView.hideCount);
    }

    @Test
    public void beingUpToDateHidesTheButton() {
        controller.onUpToDate();

        Assert.assertEquals(1, indicatorView.hideCount);
        Assert.assertTrue(updateTrigger.startedUpdates.isEmpty());
    }
}
