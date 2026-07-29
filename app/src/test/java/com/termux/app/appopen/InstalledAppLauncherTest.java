package com.termux.app.appopen;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class InstalledAppLauncherTest {

    private static final class RecordingLaunchIntentResolver implements InstalledAppLauncher.LaunchIntentResolver {
        final List<String> requestedPackageIds = new ArrayList<>();
        Intent intentToReturn;

        @Override
        public Intent resolveLaunchIntent(@NonNull String packageId) {
            requestedPackageIds.add(packageId);
            return intentToReturn;
        }
    }

    private static final class RecordingActivityStarter implements InstalledAppLauncher.ActivityStarter {
        final List<Intent> startedIntents = new ArrayList<>();

        @Override
        public void startActivity(@NonNull Intent intent) {
            startedIntents.add(intent);
        }
    }

    @Test
    public void resolvesTheLaunchIntentForThePackageIdAndStartsThatIntent() {
        RecordingLaunchIntentResolver resolver = new RecordingLaunchIntentResolver();
        Intent resolvedIntent = new Intent(Intent.ACTION_MAIN);
        resolver.intentToReturn = resolvedIntent;
        RecordingActivityStarter starter = new RecordingActivityStarter();
        InstalledAppLauncher launcher = new InstalledAppLauncher(resolver, starter);

        launcher.launchApp("com.example.app");

        Assert.assertEquals(List.of("com.example.app"), resolver.requestedPackageIds);
        Assert.assertEquals(1, starter.startedIntents.size());
        Assert.assertSame(resolvedIntent, starter.startedIntents.get(0));
    }

    @Test
    public void doesNothingWhenNoLaunchIntentResolvesForThePackageId() {
        RecordingLaunchIntentResolver resolver = new RecordingLaunchIntentResolver();
        resolver.intentToReturn = null;
        RecordingActivityStarter starter = new RecordingActivityStarter();
        InstalledAppLauncher launcher = new InstalledAppLauncher(resolver, starter);

        launcher.launchApp("com.not.installed");

        Assert.assertEquals(List.of("com.not.installed"), resolver.requestedPackageIds);
        Assert.assertTrue(starter.startedIntents.isEmpty());
    }
}
