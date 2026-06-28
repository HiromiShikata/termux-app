package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ApkUpdateFloatingIndicatorControllerTest {

    private static final class RecordingIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        final List<String> shownVersions = new ArrayList<>();
        Runnable lastTapAction;
        int hideCount;

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            shownVersions.add(latestVersionName);
            lastTapAction = onTapped;
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

    @Test
    public void showsIndicatorWithVersionWhenUpdateAvailable() {
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        ApkUpdateFloatingIndicatorController controller =
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger);

        controller.onUpdateAvailable(
            ApkUpdateAvailability.available("0.120.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        Assert.assertEquals(1, indicatorView.shownVersions.size());
        Assert.assertEquals("0.120.0", indicatorView.shownVersions.get(0));
        Assert.assertTrue(controller.hasPendingUpdate());
    }

    @Test
    public void tappingTriggersUpdateExactlyOnceThenHides() {
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        ApkUpdateFloatingIndicatorController controller =
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger);
        ApkUpdateAvailability availability =
            ApkUpdateAvailability.available("0.120.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk");
        controller.onUpdateAvailable(availability);

        indicatorView.lastTapAction.run();
        indicatorView.lastTapAction.run();

        Assert.assertEquals(1, updateTrigger.startedUpdates.size());
        Assert.assertSame(availability, updateTrigger.startedUpdates.get(0));
        Assert.assertEquals(1, indicatorView.hideCount);
        Assert.assertFalse(controller.hasPendingUpdate());
    }

    @Test
    public void hidesIndicatorWhenUpToDate() {
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        ApkUpdateFloatingIndicatorController controller =
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger);

        controller.onUpdateAvailable(ApkUpdateAvailability.upToDate("0.120.0"));

        Assert.assertTrue(indicatorView.shownVersions.isEmpty());
        Assert.assertEquals(1, indicatorView.hideCount);
        Assert.assertFalse(controller.hasPendingUpdate());
    }

    @Test
    public void hidesIndicatorAfterUpdateActionTriggered() {
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        ApkUpdateFloatingIndicatorController controller =
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger);
        controller.onUpdateAvailable(
            ApkUpdateAvailability.available("0.120.0", "https://example.com/arm64", "termux-app_arm64-v8a.apk"));

        indicatorView.lastTapAction.run();

        Assert.assertEquals(1, indicatorView.hideCount);
        Assert.assertEquals(1, updateTrigger.startedUpdates.size());
    }

    @Test
    public void tappingWithoutPendingUpdateDoesNothing() {
        RecordingIndicatorView indicatorView = new RecordingIndicatorView();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        ApkUpdateFloatingIndicatorController controller =
            new ApkUpdateFloatingIndicatorController(indicatorView, updateTrigger);

        controller.onIndicatorTapped();

        Assert.assertTrue(updateTrigger.startedUpdates.isEmpty());
        Assert.assertEquals(0, indicatorView.hideCount);
    }
}
