package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ApkUpdateNotifyDeciderTest {

    private static final ApkUpdateNotifyDecider.NotifyAction captureAction(List<ApkUpdateAvailability> sink) {
        return (ctx, availability) -> sink.add(availability);
    }

    @Test
    public void dispatchesNotificationWhenUpdateIsAvailable() {
        List<ApkUpdateAvailability> dispatched = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, captureAction(dispatched), () -> { });

        decider.onUpdateAvailable(
            ApkUpdateAvailability.available("1.1.0", "https://example.com/update.apk", "update.apk"));

        Assert.assertEquals(1, dispatched.size());
        Assert.assertEquals("1.1.0", dispatched.get(0).getLatestVersionName());
    }

    @Test
    public void doesNotDispatchNotificationWhenUpToDate() {
        List<ApkUpdateAvailability> dispatched = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, captureAction(dispatched), () -> { });

        decider.onUpToDate("1.0.0");

        Assert.assertTrue(dispatched.isEmpty());
    }

    @Test
    public void doesNotDispatchNotificationWhenCheckFailed() {
        List<ApkUpdateAvailability> dispatched = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, captureAction(dispatched), () -> { });

        decider.onCheckFailed("network error", false);

        Assert.assertTrue(dispatched.isEmpty());
    }

    @Test
    public void dispatchesVersionNameFromAvailability() {
        List<ApkUpdateAvailability> dispatched = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, captureAction(dispatched), () -> { });

        decider.onUpdateAvailable(
            ApkUpdateAvailability.available("2.5.0", "https://example.com/v2.5.0.apk", "v2.5.0.apk"));

        Assert.assertEquals("2.5.0", dispatched.get(0).getLatestVersionName());
        Assert.assertEquals("https://example.com/v2.5.0.apk", dispatched.get(0).getDownloadUrl());
    }

    @Test
    public void callsOnCompleteOnUpdateAvailable() {
        List<String> completions = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, (ctx, avail) -> { },
            () -> completions.add("done"));

        decider.onUpdateAvailable(
            ApkUpdateAvailability.available("1.1.0", "https://example.com/update.apk", "update.apk"));

        Assert.assertEquals(1, completions.size());
    }

    @Test
    public void callsOnCompleteOnUpToDate() {
        List<String> completions = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, (ctx, avail) -> { },
            () -> completions.add("done"));

        decider.onUpToDate("1.0.0");

        Assert.assertEquals(1, completions.size());
    }

    @Test
    public void callsOnCompleteOnCheckFailed() {
        List<String> completions = new ArrayList<>();
        ApkUpdateNotifyDecider decider = new ApkUpdateNotifyDecider(null, (ctx, avail) -> { },
            () -> completions.add("done"));

        decider.onCheckFailed("timeout", false);

        Assert.assertEquals(1, completions.size());
    }
}
