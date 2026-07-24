package com.termux.app.appopen;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class InstalledAppLauncher implements AppOpenTagController.AppLauncher {

    public interface LaunchIntentResolver {
        @Nullable
        Intent resolveLaunchIntent(@NonNull String packageId);
    }

    public interface ActivityStarter {
        void startActivity(@NonNull Intent intent);
    }

    private final LaunchIntentResolver mLaunchIntentResolver;

    private final ActivityStarter mActivityStarter;

    public InstalledAppLauncher(@NonNull LaunchIntentResolver launchIntentResolver,
                                @NonNull ActivityStarter activityStarter) {
        mLaunchIntentResolver = launchIntentResolver;
        mActivityStarter = activityStarter;
    }

    @Override
    public void launchApp(@NonNull String packageId) {
        Intent launchIntent = mLaunchIntentResolver.resolveLaunchIntent(packageId);
        if (launchIntent == null) return;
        mActivityStarter.startActivity(launchIntent);
    }
}
